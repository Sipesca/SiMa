/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package SincronizarFusionTables;

import Entorno.Estadisticas.Estadisticas;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.fusiontables.Fusiontables;
import com.google.api.services.fusiontables.Fusiontables.Query.Sql;
import com.google.api.services.fusiontables.FusiontablesScopes;
import com.google.api.services.fusiontables.model.Sqlresponse;
import com.google.api.services.fusiontables.model.Table;
import com.google.api.services.fusiontables.model.TableList;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import static java.lang.Thread.sleep;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Proporciona una interfaz de comunicación con las tablas en la nube mediante GOOGLE FUSION TABLES
 *
 * @author Antonio Fernández Ares (antares.es@gmail.com)
 */
public class conectarFusionTables {

  /**
   * Variable de confiruación
   */
  private static Entorno.Configuracion.Config _c = new Entorno.Configuracion.Config();
  private static Entorno.Depuracion.Debug _d = new Entorno.Depuracion.Debug();
  
  /**
   * Nombre de la aplicación registrada en Google App
   */
  private final String APPLICATION_NAME = _c.get("ft.APPLICATION_NAME");
  /**
   * Fichero donde se almacenan las credenciales de usuario
   */
  private final java.io.File DATA_STORE_FILE = new java.io.File(System.getProperty("user.home") + "/" + _c.get("directorio_configuracion") + "/client_secrets.json");
  /**
   * Directorio donde se almacenan las credenciales de usuario
   */
  private final java.io.File DATA_STORE_DIR = new java.io.File(System.getProperty("user.home") + "/" + _c.get("directorio_configuracion") + "/");
  /**
   * Instancia global del link.
   */
  private FileDataStoreFactory dataStoreFactory;
  /**
   * Instancia del transporte HTTP
   */
  private static HttpTransport httpTransport;
  /**
   * Instancia del JSON factory
   */
  private static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();
  /**
   * Instancia del manejador de fusiontable
   */
  private static Fusiontables fusiontables;
  /**
   * Variable que almacena la query insert cacheada
   */
  private String insertCache = "";
  /**
   * Variable de caché de la caché (¡Cómo en origen!)
   */
  private static Queue<String> insertCacheLista = new LinkedList<>();
  /**
   * Máximo de INSERTS a almacenar en la cache
   */
  private final int INSERT_MAX_CACHE = _c.getInt("ft.insert_cache_size");
  /**
   * Variable que indica el tamaño actual de la caché de procesamiento de inserts
   */
  private int insertCacheContador = 0;
  /**
   * Manejador de colas de peticiones a FusionTables
   */
  private static colasManejadorInsert _colas = new colasManejadorInsert();

  /**
   * Clase encargada de manejar las colas de peticiones de insercción a FusionTable
   */
  private static class colasManejadorInsert extends Thread {

    static String pendienteProcesar = null;
    Boolean vacio = false;

    public colasManejadorInsert() {
      this.setName("Manejador colas Inserts FT");
    }

    @Override
    public void run() {
      do {
        synchronized (insertCacheLista) {
          Logger.getGlobal().fine("Soy la hebra manejadora de FT " + insertCacheLista.size() + " elementos pendientes");
          if (insertCacheLista.isEmpty()) {
            try {
              insertCacheLista.wait();
              pendienteProcesar = insertCacheLista.poll();
            } catch (InterruptedException ex) {
              Logger.getGlobal().log(Level.SEVERE, null, ex);
            }
          } else if (pendienteProcesar == null || "".equals(pendienteProcesar)) {
            pendienteProcesar = insertCacheLista.poll();
          }
        }

        Logger.getGlobal().fine("Soy la hebra manejadora y voy a procesar " + pendienteProcesar);

        if (sqlStatic(pendienteProcesar) == null) {
          try {
            //Ha fallado la transación, por lo que la tenemos que volvemos a procesar pasados unos segundos
            sleep(_c.getInt("ft.tiempo_espera_error_ms"));
          } catch (InterruptedException ex) {
            Logger.getGlobal().log(Level.SEVERE, null, ex);
          }
        } else {
          //Se ha procesado correctamente
          pendienteProcesar = null;
        }

      } while (true);

    }
  }

  private Credential authorize() throws Exception {
    FileInputStream _f = new FileInputStream(DATA_STORE_FILE);

    GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(
            JSON_FACTORY,
            new InputStreamReader(_f));
    //new InputStreamReader(ejemplo.class.getResourceAsStream("/client_secrets.json")));

    if (clientSecrets.getDetails().getClientId().startsWith("Enter") || clientSecrets.getDetails().getClientSecret().startsWith("Enter ")) {
      Logger.getGlobal().log(Level.SEVERE,
              "Enter Client ID and Secret from https://code.google.com/apis/console/?api=fusiontables "
              + "into fusiontables-cmdline-sample/src/main/resources/client_secrets.json");
      System.exit(1);
    }

    GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
            httpTransport, JSON_FACTORY, clientSecrets,
            Collections.singleton(FusiontablesScopes.FUSIONTABLES)).setDataStoreFactory(
            dataStoreFactory).build();
    // authorize
    return new AuthorizationCodeInstalledApp(flow, new LocalServerReceiver()).authorize("user");
  }

  public conectarFusionTables() {
    try {
      httpTransport = GoogleNetHttpTransport.newTrustedTransport();
      dataStoreFactory = new FileDataStoreFactory(DATA_STORE_DIR);
      Credential credential = authorize();
      fusiontables = new Fusiontables.Builder(httpTransport, JSON_FACTORY, credential).setApplicationName(APPLICATION_NAME).build();
      if (!_colas.isAlive()) {
        _colas.start();
      }
    } catch (IOException ex) {
      Logger.getGlobal().log(Level.SEVERE, null, ex);
    } catch (GeneralSecurityException ex) {
      Logger.getGlobal().log(Level.SEVERE, null, ex);
    } catch (Exception ex) {
      Logger.getGlobal().log(Level.SEVERE, null, ex);
    }

  }

  public Fusiontables.Table.List listaTablas() {
    try {
      // Fetch the table list
      Fusiontables.Table.List listTables = fusiontables.table().list();
      TableList tablelist = listTables.execute();

      if (tablelist.getItems() == null || tablelist.getItems().isEmpty()) {
        Logger.getGlobal().log(Level.SEVERE, "No se encontraron tablas");
        return null;
      }

      for (Table table : tablelist.getItems()) {
        Logger.getGlobal().log(Level.INFO, table.toString());
      }

      return listTables;

    } catch (IOException ex) {
      Logger.getGlobal().log(Level.SEVERE, null, ex);
    }
    return null;
  }

  /**
   * Procesa una petición SQL básica
   *
   * @param query cadena de texto que indica la petición SQL a realizar
   * @return El archivo JSON procesado devuelto por la petición
   */
  private Sqlresponse sql(String query) {
    if (query == "" || query == null) {
      return null;
    }
    Estadisticas.PETICIONES_FT_GENERADAS++; 
   //System.err.println(query);
    
    Sqlresponse res = null;
    boolean salir = false;
    while (!salir) {
      try {
        Logger.getGlobal().fine("Procesando:" + query);
        Sql sql = fusiontables.query().sql(query);
        res = sql.execute();

        //System.err.println(res);
        //System.err.println(res.toString());
        salir = true;
        Estadisticas.PETICIONES_FT_EXITO++;
      } catch (Exception ex) {
        if (ex.getMessage().contains("403") || ex.getMessage().contains("Read timed out")) {
          try {
            sleep(_c.getInt("ft.tiempo_espera_error_ms"));
          } catch (InterruptedException ex1) {
            Logger.getGlobal().log(Level.SEVERE, "Error al dormir la hebra de Procesado de subidas a la nube", ex1);
          }
          
          salir = false;
          Logger.getGlobal().fine("Envío fallido " + ex.getMessage() + " . Demasiado rápido. Se volverá a intentar el envío");
        }else if (ex.getMessage().contains("503")) {
          //Hemos excedido la cuota
          salir = false;
          Logger.getGlobal().fine("Envío fallido " + ex.getMessage() + " . Cuota excedida. Se volverá a intentar el envío pasados " + _c.getInt("ft.tiempo.esperaSubida.dormir")/1000 + " segundos.");
          try {
            sleep(_c.getInt("ft.tiempo.esperaSubida.dormir"));
          } catch (InterruptedException ex1) {
            Logger.getGlobal().log(Level.SEVERE, "Error al dormir la hebra de Procesado de subidas a la nube", ex1);
          }
        } else {
          salir = false;
          Logger.getGlobal().log(Level.SEVERE, "Envío fallido " + ex.getMessage() + "'" + query + "'", ex);
          //¿Habría que hacer que no se quedase aquí? Por si algún día falla... por algo xD
        }
        
      }
    }
    Logger.getGlobal().fine("Procesado correctamente.");
    return res;

  }

  /**
   * Procesa una petición SQL básica en un contexto static
   *
   * @param query cadena de texto que indica la petición SQL a realizar
   * @return El archivo JSON procesado devuelto por la petición
   */
  private static Sqlresponse sqlStatic(String query) {
    Logger.getGlobal().fine("Preparando envío");

    if (query == null || "".equals(query)) {
      return null;
    } else {
      Estadisticas.PETICIONES_FT_GENERADAS++; 
      Sqlresponse res = null;
      try {
        Sql sql = fusiontables.query().sql(query);
        res = sql.execute();
        Logger.getGlobal().fine("Envío realizado correctamente.");
        Estadisticas.PETICIONES_FT_EXITO++; 

      } catch (IOException ex) {
        //System.err.println(ex.getMessage());
        if (ex.getMessage().contains("403") || ex.getMessage().contains("Read timed out") || ex.getMessage().contains("503")) {
          Logger.getGlobal().fine("Envío fallido " + ex.getMessage() + " . Se volverá a intentar el envío");
        } else {
          Logger.getGlobal().log(Level.SEVERE, "Envío estático fallido " + ex.getMessage() + " '" + query + "'", ex);
        }

        //Logger.getLogger(conectarFusionTables.class.getName()).log(Level.SEVERE, null, ex);
        return null;
      }
      return res;
    }
  }

  /**
   * Función que realiza una selección de una tabla
   *
   * @param tabla Identificador de la tabla que se quiere consultar
   * @param campos Campos que queremos recuperar
   * @param condiciones Condiciones que tiene que cumplir las tuplas para que sean devueltas
   * @return
   */
  public Sqlresponse select(String tabla, String campos, String condiciones) {
    return sql("SELECT " + campos + " FROM " + tabla + " WHERE " + condiciones);
  }

  /**
   * Función que realiza una selección de una tabla con condiciones extras
   *
   * @param tabla Identificador de la tabla que se quiere consultar
   * @param campos Campos que queremos recuperar
   * @param condiciones Condiciones que tiene que cumplir las tuplas para que sean devueltas
   * @return
   */
  public Sqlresponse select(String tabla, String campos, String condiciones, String extras) {
    if (!"".equals(condiciones)) {
      return sql("SELECT " + campos + " FROM " + tabla + " WHERE " + condiciones + " " + extras);
    } else {
      return sql("SELECT " + campos + " FROM " + tabla + " " + extras);
    }
  }

  /**
   * Función que inserta una nueva tupla en la tabla alojada en FusionTables
   *
   * @param tabla identificador de la tabla
   * @param campos listado de campos (separads por comas) de la tabla
   * @param valores listado de valores (separados por comas) a insertar
   * @param check si es necesario realizar comprobación de insercción para en caso de ocurrencia usar UPDATE
   * @return La respuesta devuelta por el servidor de Fusion Tables.
   */
  public Sqlresponse insert(String tabla, List<String> campos, List<String> valores, boolean check) {
    String peticion;
    if (check) {
      peticion = "SELECT ROWID FROM " + tabla + " WHERE ";
      for (int i = 0; i < campos.size(); i++) {
        if (i != 0) {
          peticion = peticion + " AND ";
        }
        peticion = peticion + campos.get(i) + "=\'" + valores.get(i) + "\'";
      }

      //System.err.println(peticion);
      Sqlresponse s = this.sql(peticion);
      //System.err.println(s.size());

      //Si el tamaño del MAP es 2, sólo se han devuelto el identificador de la consulta y las columnas, NO los valores.
      //Por tanto, no hay valores. Hace un s.getRows().isEmpty() no funciona.
      if (s.size() == 2) {
        peticion = "INSERT INTO " + tabla + "(";

        for (int i = 0; i < campos.size(); i++) {
          if (i != 0) {
            peticion = peticion + ",";
          }
          peticion = peticion + campos.get(i);
        }

        peticion = peticion + ") VALUES (";

        for (int i = 0; i < valores.size(); i++) {
          if (i != 0) {
            peticion = peticion + ",";
          }
          peticion = peticion + "\'" + valores.get(i) + "\'";
        }
        peticion = peticion + ");";

        insertCache = insertCache + peticion;
        insertCacheContador++;
        //return this.sql(peticion);

      } else {
        return this.update(tabla, campos, valores, s.getRows());
      }

    } else {
      peticion = "INSERT INTO " + tabla + "(";

      for (int i = 0; i < campos.size(); i++) {
        if (i != 0) {
          peticion = peticion + ",";
        }
        peticion = peticion + campos.get(i);
      }

      peticion = peticion + ") VALUES (";

      for (int i = 0; i < valores.size(); i++) {
        if (i != 0) {
          peticion = peticion + ",";
        }
        peticion = peticion + "\'" + valores.get(i) + "\'";
      }
      peticion = peticion + ");";
      insertCache = insertCache + peticion;
      insertCacheContador++;
      //return this.sql(peticion);
    }

    sync();
    return null;

  }

  /**
   * Función que inserta una nueva tupla en la tabla alojada en FusionTables indicando el número de condiciones de
   * comprobación
   *
   * @param tabla identificador de la tabla
   * @param campos listado de campos (separads por comas) de la tabla
   * @param valores listado de valores (separados por comas) a insertar
   * @param check si es necesario realizar comprobación de insercción para en caso de ocurrencia usar UPDATE
   * @param para número de campos de la lista valores a utilizar en la comparación en caso de que check sea verdadero
   * @return La respuesta devuelta por el servidor de Fusion Tables.
   */
  public Sqlresponse insert(String tabla, List<String> campos, List<String> valores, boolean check, int para) {
    String peticion;
    if (check) {
      peticion = "SELECT ROWID ";
      for(int i = para ; i < campos.size();i++){
        peticion = peticion + "," + campos.get(i);
      }
      peticion = peticion + " FROM " + tabla + " WHERE ";
      for (int i = 0; i < para; i++) {
        if (i != 0) {
          peticion = peticion + " AND ";
        }
        peticion = peticion + campos.get(i) + "=\'" + valores.get(i) + "\'";
      }

      //System.err.println(peticion);
      Sqlresponse s = this.sql(peticion);
      //System.err.println(s.size());

      //Si el tamaño del MAP es 2, sólo se han devuelto el identificador de la consulta y las columnas, NO los valores.
      //Por tanto, no hay valores. Hace un s.getRows().isEmpty() no funciona.
      if (s.size() == 2) {
        peticion = "INSERT INTO " + tabla + "(";

        for (int i = 0; i < campos.size(); i++) {
          if (i != 0) {
            peticion = peticion + ",";
          }
          peticion = peticion + campos.get(i);
        }

        peticion = peticion + ") VALUES (";

        for (int i = 0; i < valores.size(); i++) {
          if (i != 0) {
            peticion = peticion + ",";
          }
          peticion = peticion + "\'" + valores.get(i) + "\'";
        }
        peticion = peticion + ");";

        insertCache = insertCache + peticion;
        insertCacheContador++;
        //return this.sql(peticion);

      } else {
        boolean cambio = false;
        int j = 1;
        for(int i = para; i< campos.size(); i ++){
          if(valores.get(i)==s.getRows().get(0).get(j)){
            cambio = true;
            i = Integer.MAX_VALUE; //Salimos del bucle, se acabó el buscar.
          }
          j++; //En 0 está ROWID
        }
        if(cambio){
        return this.update(tabla, campos, valores, s.getRows());
        }else{
          return null;
        }
        
      }

    } else {
      peticion = "INSERT INTO " + tabla + "(";

      for (int i = 0; i < campos.size(); i++) {
        if (i != 0) {
          peticion = peticion + ",";
        }
        peticion = peticion + campos.get(i);
      }

      peticion = peticion + ") VALUES (";

      for (int i = 0; i < valores.size(); i++) {
        if (i != 0) {
          peticion = peticion + ",";
        }
        peticion = peticion + "\'" + valores.get(i) + "\'";
      }
      peticion = peticion + ");";
      insertCache = insertCache + peticion;
      insertCacheContador++;
      //return this.sql(peticion);
    }

    sync();
    return null;

  }

  public String status() {
    return "Sincronización FT" + " Cache: " + insertCacheContador;
  }

  public synchronized void sync() {
    if (insertCacheContador >= INSERT_MAX_CACHE) {
      synchronized (insertCacheLista) {
        //Sqlresponse res = this.sql(insertCache);
        insertCacheLista.add(insertCache);
        insertCacheLista.notify();
        insertCacheContador = 0;
        insertCache = "";
      }
    }
  }

  public synchronized void forzarSync() {
    synchronized (insertCacheLista) {
      insertCacheLista.add(insertCache);
      insertCacheLista.notify();
      insertCacheContador = 0;
      insertCache = "";
    }
  }

  public void esperarSubida() {
    
    while (!insertCacheLista.isEmpty()){
      Logger.getGlobal().fine("Quedan (estoy dentro) " + insertCacheLista.size() + " peticiones de subida por procesar.");
      try {
        sleep(_c.getInt("ft.tiempo.esperaSubida.dormir"));
      } catch (InterruptedException ex) {
        Logger.getGlobal().log(Level.SEVERE, null, ex);
      }
    }
  }

  /**
   * Sobrecarga del método insert para no indicar que se compruebe que existe el elemento. Por defecto, el elemento a
   * insertar se comprueba si existe en la base de datos de FUSION TABLES antes de insertarlo
   *
   * @param tabla identificador de la tabla
   * @param campos listado de campos (separads por comas) de la tabla
   * @param valores listado de valores (separados por comas) a insertar
   * @return La respuesta devuelta por el servidor de Fusion Tables.
   */
  public Sqlresponse insert(String tabla, List<String> campos, List<String> valores) {
    return insert(tabla, campos, valores, true);
  }

  /**
   * Sobrecarga del método insert para no indicar que se compruebe que existe el elemento y para un solo elemento. Por
   * defecto, el elemento a insertar se comprueba si existe en la base de datos de FUSION TABLES antes de insertarlo
   *
   * @param tabla identificador de la tabla
   * @param campos listado de campos (separads por comas) de la tabla
   * @param valores listado de valores (separados por comas) a insertar
   * @return La respuesta devuelta por el servidor de Fusion Tables.
   */
  public Sqlresponse insert(String tabla, String campos, String valores) {
    List<String> c = new ArrayList<>();
    List<String> v = new ArrayList<>();

    c.add(campos);
    v.add(valores);

    return insert(tabla, c, v, true);
  }

  /**
   * Sobrecarga del método insert para un solo elemento.
   *
   * @param tabla identificador de la tabla
   * @param campos listado de campos (separads por comas) de la tabla
   * @param valores listado de valores (separados por comas) a insertar
   * @return La respuesta devuelta por el servidor de Fusion Tables.
   */
  public Sqlresponse insert(String tabla, String campos, String valores, boolean check) {
    List<String> c = new ArrayList<>();
    List<String> v = new ArrayList<>();

    c.add(campos);
    v.add(valores);

    return insert(tabla, c, v, check);
  }

  /**
   * Función que actualiza una tupla en la tabla alojada en FusionTables
   *
   * @param tabla identificador de la tabla
   * @param campos campo que será actualizado
   * @param valores valor que será actualizado
   * @param ROWID identificador de la tupla
   * @return
   */
  public Sqlresponse update(String tabla, List<String> campos, List<String> valores, List<List<Object>> ROWIDs) {
    for (Object ite : ROWIDs) {

      String peticion = "UPDATE " + tabla + " SET ";

      for (int i = 0; i < campos.size(); i++) {
        if (i > 0) {
          peticion = peticion + ",";
        }
        peticion = peticion + campos.get(i) + " = \'" + valores.get(i) + "\'";
      }
      peticion = peticion + "  WHERE ROWID = " + "\'" + ((String) ((List<String>) ite).get(0)) + "\'; ";

      this.sql(peticion);
    }
    return null;
  }

  /**
   * Función que elimina una tupla en la tabla alojada en FusionTables
   *
   * @param tabla identificador de la tabla
   * @param ROWID identificador de la tupla
   * @return
   */
  public Sqlresponse delete(String tabla, String ROWID) {
    String peticion = "DELETE FROM " + tabla + "\" WHERE ROWID = " + "\'" + ROWID + "\'";
    return this.sql(peticion);
  }

  /**
   * Función que elimina una tupla en la tabla alojada en FusionTables
   *
   * @param tabla identificador de la tabla
   * @param ROWID identificador de la tupla
   * @return
   */
  public Sqlresponse delete(String tabla, List<List<Object>> ROWIDs) {
    String peticion;

    for (Object ite : ROWIDs) {
      peticion = "DELETE FROM " + tabla + "\" WHERE ROWID = " + "\'" + ((String) ((List<String>) ite).get(0)) + "\';";
      this.sql(peticion);
    }

    return null;
  }
}
