/*
 * Clase para la conexión con la base de datos local
 */
package Entorno.Conectar;

import java.sql.*;
import Entorno.Configuracion.Config;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * Manejador de la conexión a MySQL con el servidor local
 * @author mgarenas, Antonio Fernández Ares (antares.es@gmail.com)
 */
public class Conectar {

    Config _c = new Config();
    public static int current_conections = 0;
    
    Connection conn = null;

    public Conectar() {
      if(conn == null){
      //if(current_conections < 100){
        try {
            //Indicamos el driver y lo instanciamos
            Class.forName("com.mysql.jdbc.Driver").newInstance();
            //Creamos la conexion
            String host = _c.get("db.host");
            String base = _c.get("db.basedatos");
            String usuario = _c.get("db.usuario");
            String pass = _c.get("db.contraseña");
            
            conn = DriverManager.getConnection("jdbc:mysql://"+host+"/"+base,usuario, pass);//el tercer parametro es para el password
            current_conections++;
        } catch (Exception e) {
          Logger.getGlobal().log(Level.SEVERE,"Error instanciando conexión a la base de datos", e);
          conn = null;
        }
    //}
    }
    }

    public void cerrar() throws SQLException {
        if(conn != null){
        current_conections--;
        conn.close();//se cierra la conexión
        conn = null;
        }
    }

    public Statement crearSt() throws SQLException {
        return conn.createStatement();
    }
}
