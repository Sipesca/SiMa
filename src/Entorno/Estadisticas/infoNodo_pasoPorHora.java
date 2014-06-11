/*
 * Copyright (C) 2014 antares
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
package Entorno.Estadisticas;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 *
 * @author antares
 */
public class infoNodo_pasoPorHora {

  public class info {
    public String idNodo = "";
    public int valor = 0;
    public String nombreNodo = "";
    public String tipoNodo = "";
  }
  
  public static Map<String,info> _contenedor = new HashMap<>(100);
  

  public infoNodo_pasoPorHora() {
  }

  public static void reset() {
    idNodo = "";
    valor = 0;
    nombreNodo = "";
    tipoNodo = "";
  }

  /**
   * Establece SIN comprobación
   *
   * @param _idNodo
   * @param _valor
   * @param _nombreNodo
   */
  public static void set(String _idNodo, int _valor, String _nombreNodo) {
    Logger.getGlobal().fine("Solicitud actualización estadística mejor nodo" + _idNodo + " " + _valor + " " + _nombreNodo);
    String[] nombres = _nombreNodo.split(" ");
    if (nombres.length > 0 && nombres[0].equals("Sipesca")) {
      tipoNodo = nombres[2];
      nombreNodo = "";
      for (int i = 3; i < nombres.length; i++) {
        nombreNodo += nombres[i] + " ";
      }
      idNodo = _idNodo;
      valor = _valor;

      Logger.getGlobal().fine("Actualizada estadística mejor nodo" + infoNodo_pasoPorHora.prime());
    }
  }

  /**
   * Estable CON comprobación
   *
   * @param _idNodo
   * @param _valor
   * @param _nombreNodo
   */
  public static void update(String _idNodo, int _valor, String _nombreNodo) {
    if (_valor > valor) {
      String[] nombres = _nombreNodo.split(" ");
      if (nombres.length > 0 && nombres[0].equals("Sipesca")) {
        tipoNodo = nombres[2];
        nombreNodo = "";
        for (int i = 3; i < nombres.length; i++) {
          nombreNodo += nombres[i] + " ";
        }
        idNodo = _idNodo;
        valor = _valor;
      }
    }
  }

  public static String prime() {
    return "Detectados " + valor + " dispositivos " + tipoNodo + " en " + nombreNodo + "en la última hora. Más en info en http://sipesca.ugr.es/panel";
  }
}
