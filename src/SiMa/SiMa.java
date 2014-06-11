/*
 * SiMa : Sipesca Manager
 */
package SiMa;

import ActualizadorLocal.ActualizadorDBLocal;
import SincronizarFusionTables.ActualizadorFT;
import Entorno.Configuracion.Config;
import Entorno.Depuracion.Debug;
import Entorno.Estadisticas.Estadisticas;
import Prediccion.Prediccion;
import java.io.IOException;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import java.util.logging.XMLFormatter;
import util.TwitterAgente;


/**
 * Una clase para gobernarlas a todas, una clase para encontrarlas, una clase para atraerlas a todas y atarlas en las
 * tinieblas
 *
 * @author Antonio Fernández Ares (antares.es@gmail.com)
 */
public class SiMa {

  /**
   * Cargamos la configuración
   */
  static Config _c = new Config();
  /**
   * Cargamos variable de depuración
   */
  static Debug _d = new Debug();
  
  static Estadisticas _e = new Estadisticas();
  /**
   * Manejador de la actualización en Córdoba
   */
  static ActualizadorLocal.ActualizadorDBLocal _actualizarDB;
  /**
   * Manejador de la actualización en FusionTable
   */
  static ActualizadorFT _actualizarFT;
  /**
   * Manejador de grupos de hebras
   */
  static ThreadGroup tg;

  /**
   * Método principal de la clase
   *
   * @param args Los argumentos de ejecución (Actualmente no utilizados)
   */
  public static void main(String[] args) throws IOException {
    
    
    
    //Generando configuración de depuración
    _d.generarConfiguracion();
    Logger.getGlobal().info("Comenzando ejecución. Configuración Cargada.");
    
   //Prediccion p = new Prediccion("1351591800440");
   //p.start();   
   
   Thread.currentThread().setName("Sipesca - Proceso principal");
   
    tg = Thread.currentThread().getThreadGroup();

    _actualizarDB = new ActualizadorDBLocal(_c.get("data.ultimo"));
    _actualizarFT = new ActualizadorFT();

    Logger.getGlobal().info("Comenzando proceso de sincronizado LOCAL.");
    _actualizarDB.start();
    
    Logger.getGlobal().info("Comenzando proceso de sincronizado NUBE.");
    _actualizarFT.start(); 
    

    TwitterAgente _t = new TwitterAgente();
    _t.publicar("Seguímos haciendo pruebas, En el envío de información automática. Perdón por las molestias.");

      try {
      //_actualizarFT.join();
      _actualizarDB.join(); // ----- DESCOMENTAR ANTES DE LANZAR
        //p.join();
      } catch (InterruptedException ex) {
      Logger.getGlobal().severe("Apocalipsis " + ex.getMessage());
      }
    
  }
}
