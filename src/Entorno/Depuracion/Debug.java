/**
 * Depuracion Paquete que proporciona herramientas para la depuración por pantalla de la aplicación
 *
 */
package Entorno.Depuracion;

import Entorno.Configuracion.Config;
import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.ConsoleHandler;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.XMLFormatter;

/**
 * Clase encargada de las tareas de depuración y esas cosas
 *
 * @author Antonio Fernández Ares (antares.es@gmail.com)
 */
public class Debug {

  /**
   * Variable de configuración
   */
  private static Config _c = new Config();
  /**
   * Variables para medida de tiempo
   */
  static long t_start, time;
  /**
   * Variable de formato para número flotantes
   */
  public static DecimalFormat df = new DecimalFormat("#.####");
  /**
   * Variable de formato para fechas
   */
  public static SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");

  /**
   * Función de depuración: Devuele una cadena con la hora y fecha actuales formateadas
   *
   * @return Marca con el tiempo actual
   */
  public static String timeMarca() {
    return "{" + sdf.format(new Date(System.currentTimeMillis())) + "}";
  }

  /**
   * Función de depuración: Almacena el tiempo actual para realizar una medición posterior
   *
   */
  public void timeCheck() {
    t_start = System.currentTimeMillis();
  }

 
  /**
   * Devuelve el modo de salida por pantalla
   *
   * @return True - Si el modo Verbose está activado. False -Si el modo Verbose está desactivado
   */
  @Deprecated
  public boolean isVerbose() {
    return _c.getBool("debug");
  }

  /**
   * Función de depuración: Imprime el tiempo transcurrido desde la última marca de tiempo
   *
   * @param reset Booleano que indica si se tiene que es establecer una nueva marca de tiempo tras realizar la medición
   * @return Cadena de texto con la medición de tiempo
   */
  @Deprecated
  public String timeDisplay(boolean reset) {
    time = System.currentTimeMillis() - t_start;
    String t = "(" + df.format(time / 1000.0 / 60.0) + "min)";
    if (reset) {
      t_start = System.currentTimeMillis();
    }
    return t;
  }

  /**
   * Salida estándar del mensaje cad con una marca de tiempo
   *
   * @param cad mensaje a mostrar por salida estándar
   */
  @Deprecated
  public void primeOUT(String cad) {
    System.out.println(timeMarca() + " " + cad);
  }

  /**
   * Salida de depuración del mensaje cad con una marca de tiempo
   *
   * @param cad mensaje a mostrar por salida estándar
   */
  @Deprecated
  public void primeVerbose(String cad) {
    if (this.isVerbose()) {
      System.out.println(timeMarca() + " " + cad);
    }
  }

  /**
   * Salida de eror del mensaje cad con una marca de tiempo
   *
   * @param cad mensaje a mostrar por salida estándar
   */
  @Deprecated
  public void primeERR(String cad) {
    System.err.println("E>" + timeMarca() + " " + cad);
  }

  /**
   * Salida estándar del mensaje cad con una marca de tiempo y una etiqueta
   *
   * @param cad mensaje a mostrar por salida estándar
   */
  @Deprecated
  public void primeOUT(String label, String cad) {
    System.out.println(timeMarca() + "[" + label + "] " + cad);
  }

  /**
   * Salida de depuración del mensaje cad con una marca de tiempo y una etiqueta
   *
   * @param cad mensaje a mostrar por salida estándar
   */
  @Deprecated
  public void primeVerbose(String label, String cad) {
    if (this.isVerbose()) {
      System.out.println(timeMarca() + "[" + label + "] " + cad);
    }
  }

  /**
   * Salida de error del mensaje cad con una marca de tiempo y una etiqueta
   *
   * @param cad mensaje a mostrar por salida estándar
   */
  @Deprecated
  public void primeERR(String label, String cad) {
    System.err.println("E>" + timeMarca() + "[" + label + "] " + cad);
  }
  
  public void generarConfiguracion(){
        try {
      //Creamos el directorio de logs si no existe
      File dir_logs = new File("logs");
      if (!dir_logs.exists()) {
        dir_logs.mkdir();
      }
     
      Logger.getAnonymousLogger().setLevel(Level.ALL);
      Logger.getGlobal().setLevel(Level.ALL);
      
      //Fichero de log global
      FileHandler f_global = new FileHandler("logs/log", 314572800, 6, true);
      f_global.setLevel(Level.ALL);
      f_global.setFormatter(new formatoLog());

      Logger.getGlobal().addHandler(f_global);
      Logger.getGlobal().removeHandler(new ConsoleHandler());
      
      //Fichero de log global en XML
      FileHandler f_global_XML = new FileHandler("logs/xml", 314572800, 6, true);
      f_global_XML.setLevel(Level.WARNING);
      f_global_XML.setFormatter(new XMLFormatter());

      Logger.getGlobal().addHandler(f_global_XML);
      
      //Fichero dedicado para los errores
      FileHandler f_global_error = new FileHandler("logs/error", 314572800, 3, true);
      f_global_error.setLevel(Level.SEVERE);
      f_global_error.setFormatter(new formatoLog());

      Logger.getGlobal().addHandler(f_global_error);
      
      
       //Fichero dedicado para los errores
      FileHandler f_info = new FileHandler("logs/info", 314572800, 3, true);
      f_info.setLevel(Level.INFO);
      f_info.setFormatter(new formatoLog());

      Logger.getGlobal().addHandler(f_info);
      

    } catch (IOException | SecurityException ex) {
      Logger.getGlobal().log(Level.SEVERE, null, ex);
    }
  }
   
  /**
   * Constructor de la clase de depuración
   */
  public Debug() {

  }
}
