/* 
 * Copyright (C) 2013 antares
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package ActualizadorLocal.Clientes;

import Entorno.Conectar.Conectar;
import Entorno.Configuracion.Config;
import Entorno.Depuracion.Debug;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.UniformInterfaceException;
import com.sun.jersey.api.client.WebResource;
import java.sql.SQLException;
import java.sql.Statement;
import javax.net.ssl.*;
import javax.ws.rs.core.MultivaluedMap;
import static java.lang.Thread.sleep;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

/**
 *
 * @author mgarenas, Antonio Fernández Ares (antares.es@gmail.com)
 */
public class ClientePasos {

  private Debug _d = new Debug();
  private Config _c = new Config();
  private WebResource webResource;
  private Client client;
  Conectar conexion;
  String[] queryParamNames;
  String[] queryParamValues;
  String query;
  int insertados = 0;
  int procesados = 0;
  //Variables de cache de base de datos
  String cache = "";
  int cache_size = 0;
  int MAX_CACHE_SIZE = _c.getInt("db.PASO.MAX_CACHE_SIZE");
  boolean real_time = true;
  //Variables de multihebrado
  public List<threadSyncDB> l_th = new ArrayList<>();
  public String label;
  public long TIME_SLEEP_IN_ERROR = _c.getInt("db.PASO.TIME_SLEEP_IN_ERROR");
  public int MAX_HEBRAS_ACTIVAS_SIMULTANEAS = _c.getInt("db.PASO.MAX_HEBRAS_ACTIVAS_SIMULTANEAS");
  public int MAX_ERRORES_PARA_NOTIFICACION = _c.getInt("db.PASO.MAX_ERRORES_PARA_NOTIFICACION");

  public ClientePasos(String start, String end) {
    queryParamNames = new String[]{"user", "pass", "start", "end"};
    queryParamValues = new String[]{_c.get("sc.USER"), _c.get("sc.PASS"), start, end};
    com.sun.jersey.api.client.config.ClientConfig config = new com.sun.jersey.api.client.config.DefaultClientConfig(); // SSL configuration
    // SSL configuration
    config.getProperties().put(com.sun.jersey.client.urlconnection.HTTPSProperties.PROPERTY_HTTPS_PROPERTIES, new com.sun.jersey.client.urlconnection.HTTPSProperties(getHostnameVerifier(), getSSLContext()));
    client = Client.create(config);

  }

  public void setLabel(String label) {
    this.label = label;
  }

  public int getProcesados() {
    return procesados;
  }

  public int getInsertados() {
    return insertados;
  }

  public <T> T get_Pasos(Class<T> responseType) throws UniformInterfaceException {
    return webResource.get(responseType);
  }

  public void createWebResource(String node) {

    query = "http://cityanalytics.net/restapi/rawdataservice/"
            + node + "/pasos?user=" + queryParamValues[0] + "&pass=" + queryParamValues[1] + "&start="
            + queryParamValues[3] + "&end="
            + queryParamValues[2];

    //System.err.println(query);

    webResource = client.resource("http://cityanalytics.net/restapi/rawdataservice/"
            + node + "/pasos?user=" + queryParamValues[0] + "&pass=" + queryParamValues[1] + "&start="
            + queryParamValues[3] + "&end="
            + queryParamValues[2]);
  }

  public void procesarDatos(String datos) throws SQLException {

    datos = "{\"pasos\":" + datos + "}";

    JSONParser parser = new JSONParser();

    try {

      JSONObject obj = (JSONObject) parser.parse(datos);
      JSONArray lista = (JSONArray) obj.get("pasos");
      procesados = lista.size();

      int conta = 1;
      int lotes = procesados / MAX_CACHE_SIZE + 1;

      for (int i = 0; i < lista.size(); i++) {
        if (i % MAX_CACHE_SIZE == 0) {
          Logger.getGlobal().log(Level.FINE, "Procesando Pasos. Lote " + conta + " de " + lotes);
          conta++;
        }
        Long a0 = (Long) ((JSONObject) lista.get(i)).get("idNodo");
        String a1 = (String) ((JSONObject) lista.get(i)).get("idDispositivo");
        Long a2 = (Long) ((JSONObject) lista.get(i)).get("tfin");
        Long a3 = (Long) ((JSONObject) lista.get(i)).get("tinicio");

        this.InsertarDatos("\"" + (String) Long.toString(a0) + "\",\"" + a1 + "\",\"" + Long.toString(a2) + "\",\"" + Long.toString(a3) + "\"");

      }

    } catch (Exception e) {
      Logger.getGlobal().log(Level.SEVERE, "Error en el procesado de los datos.", e);
    }

    syncDB();

  }

  @Deprecated
  public void procesarDatosSplit(String datos) throws SQLException {

    String datosAInsertar = "";
    datos = "," + datos;
    String[] result = datos.toString().split("}");
    for (int x = 0; x < result.length - 1; x++) {
      String[] result2 = result[x].split(",");
      for (int y = 1; y < result2.length; y++) {
        String[] result3 = result2[y].split(":");
        //for (int w=1; w<result3.length; w++){
        datosAInsertar += result3[1] + ", ";
        //System.err.println("x,y"+x+", "+y+" "+result3[1] + "|\t|" + datosAInsertar);
        //}
      }

      this.InsertarDatos(datosAInsertar.substring(0, datosAInsertar.lastIndexOf(",")));
      datosAInsertar = "";
    }

  }

  public String getQuery() {
    return query;
  }

  public void setConexion(Conectar connect) {
    this.conexion = connect;
  }

  public void borrarDatosTablaPasos() throws SQLException {
    /*Statement st = conexion.crearSt();
     st.execute("Alter table paso disable keys;");
     st = conexion.crearSt();
     st.executeUpdate("Delete from paso;");
     */
    return;
  }

  public void InsertarDatosSync(String datos) throws SQLException {
    try {
      Statement st = conexion.crearSt();

      //System.err.println("INSERT INTO paso (idNodo, idDispositivo, tinicio, tfin) VALUES (" + datos + " )");
      st.executeUpdate("INSERT INTO __paso (idNodo, idDispositivo, tinicio, tfin) VALUES (" + datos + " )");
      insertados++;
    } catch (com.mysql.jdbc.exceptions.MySQLIntegrityConstraintViolationException e) {
      return;
    }

  }

  public void InsertarDatos(String datos) {
    cache = cache + (cache_size != 0 ? "," : "") + " (" + datos + " ) ";
    cache_size++;

    if (cache_size >= MAX_CACHE_SIZE) {
      syncDB();
    }
  }

  public class threadSyncDB extends Thread {

    String query;
    private Conectar c;
    int intentos = 0;
    boolean procesada = false;
    int insertados = 0;
    int id;

    public threadSyncDB(String cache, int i) {
      query = cache;
      id = i;
    }

    @Override
    public void run() {
      do {
        try {
          //Que espere la anterior
          this.c = new Conectar();
          Statement st = c.crearSt();
          insertados = st.executeUpdate("INSERT IGNORE INTO __paso (idNodo, idDispositivo, tfin, tinicio) VALUES" + query + ";");
          procesada = true;
          c.cerrar();
        } catch (SQLException ex) {
          procesada = false;
          intentos++;
          if (intentos > MAX_ERRORES_PARA_NOTIFICACION) {
            Logger.getGlobal().log(Level.WARNING, "Aviso hebra " + this.getId() + " no ha sincronizado aún con DB. Código " + ex.getErrorCode() + " Se intentará nuevamente en " +(TIME_SLEEP_IN_ERROR * intentos * (int) Math.log(Conectar.current_conections))/1000 +  " segundos (Intentos: " + intentos + ")[Cache:" + cache_size + "][Conexiones: " + Conectar.current_conections + "]"  + "[Hebras: " + l_th.size() + "]");
          }
          try {
            //Meter un random
            sleep(TIME_SLEEP_IN_ERROR * intentos * (int) Math.log(Conectar.current_conections));
          } catch (InterruptedException ex1) {
            Logger.getGlobal().log(Level.SEVERE, "Error durmiendo hebra " + this.getId(), ex1);
          }

        } /*catch (NullPointerException e) {
         procesada = false;
         intentos++;

         Logger.getGlobal().log(Level.INFO, "Error (NULL) hebra " + this.getId() + " no se ha podido conectar a la DB. Se intentará nuevamente (" + intentos + ")[" +cache_size  + "]" + "[" + l_th.size() + "]" );
         try {
         sleep(TIME_SLEEP_IN_ERROR);
         } catch (InterruptedException ex1) {
         Logger.getGlobal().log(Level.SEVERE, "Error durmiendo hebra " + this.getId(), ex1);
         }
         } */ catch (Exception ex) {
          procesada = false;
          intentos++;
          if (intentos > MAX_ERRORES_PARA_NOTIFICACION) {
            Logger.getGlobal().log(Level.WARNING, "Aviso hebra " + this.getId() + " no ha podido reservar conexión aún a la DB. Se intentará nuevamente en " + ((int) TIME_SLEEP_IN_ERROR * intentos * (int) Math.log(Conectar.current_conections)/1000) + " segundos (Intentos: " + intentos + ")[Cache:" + cache_size + "][Conexiones: " + Conectar.current_conections + "]"  + "[Hebras: " + l_th.size() + "]");
          }
          try {
            sleep((int) TIME_SLEEP_IN_ERROR * intentos * (int) Math.log(Conectar.current_conections));
          } catch (InterruptedException ex1) {
            Logger.getGlobal().log(Level.SEVERE, "Error durmiendo hebra " + this.getId(), ex1);
          }

        }


      } while (!procesada);

    }

    @Override
    protected void finalize() throws Throwable {
      //c.cerrar();
      conexion.cerrar();
      super.finalize(); //To change body of generated methods, choose Tools | Templates.
    }
  }

  public void syncDB() {
    if (cache_size != 0) {
      try {
        l_th.add(new threadSyncDB(cache, l_th.size()));
        l_th.get(l_th.size() - 1).start();

      } catch (Exception ex) {
        Logger.getGlobal().log(Level.SEVERE, "Error creando hebra sincronización con la DB", ex);

      }
    }
    cache_size = 0;
    cache = "";
  }

  public class threadCierre extends Thread {

    private List<threadSyncDB> l = null;

    public threadCierre(List<threadSyncDB> _l) {
      this.l = _l;
    }

    @Override
    public void run() {
      int insertados = 0;
      Logger.getGlobal().log(Level.FINE, "Escritura Pasos en BD: " + l.size() + " peticiones");
      while (!l.isEmpty()) {
        try {
          l.get(0).join();
          insertados = insertados + l.get(0).insertados;
          l.remove(0);
        } catch (InterruptedException ex) {
          Logger.getGlobal().log(Level.SEVERE, null, ex);
        }

      }
      Logger.getGlobal().log(Level.FINE, "Escritura Pasos en DB OK. Dispositivos insertados/procesados/: " + insertados + "/" + procesados);

    }
    
            /**
         * Función que se encarga de matar a la hebra.
         * @throws Throwable 
         */
        @Override
        protected void finalize() throws Throwable {
            conexion.cerrar();
            l.clear();
            super.finalize();
        }
    
  }

  public void close() {
    threadCierre t = new threadCierre(l_th);
    t.start();
    client.destroy();
  }

  private HostnameVerifier getHostnameVerifier() {
    return new HostnameVerifier() {
      @Override
      public boolean verify(String hostname, javax.net.ssl.SSLSession sslSession) {
        return true;
      }
    };
  }

  private MultivaluedMap getQueryOrFormParams(String[] paramNames, String[] paramValues) {
    MultivaluedMap<String, String> qParams = new com.sun.jersey.api.representation.Form();
    for (int i = 0; i < paramNames.length; i++) {
      if (paramValues[i] != null) {
        qParams.add(paramNames[i], paramValues[i]);
      }
    }
    //System.err.println("P>" + qParams.toString());
    return qParams;
  }

  private SSLContext getSSLContext() {
    javax.net.ssl.TrustManager x509;
    x509 = new javax.net.ssl.X509TrustManager() {
      @Override
      public void checkClientTrusted(java.security.cert.X509Certificate[] arg0, String arg1) throws java.security.cert.CertificateException {
      }

      @Override
      public void checkServerTrusted(java.security.cert.X509Certificate[] arg0, String arg1) throws java.security.cert.CertificateException {
      }

      @Override
      public java.security.cert.X509Certificate[] getAcceptedIssuers() {
        return null;
      }
    };
    SSLContext ctx = null;
    try {
      ctx = SSLContext.getInstance("SSL");
      ctx.init(null, new javax.net.ssl.TrustManager[]{x509}, null);
    } catch (java.security.GeneralSecurityException ex) {
    }
    return ctx;
  }
}
