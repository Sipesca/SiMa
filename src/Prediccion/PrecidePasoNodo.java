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
package Prediccion;

import Entorno.Configuracion.Config;
import Entorno.Depuracion.Debug;
import SincronizarFusionTables.conectarFusionTables;

import com.google.api.services.fusiontables.model.Sqlresponse;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import weka.core.Instances;
import weka.classifiers.evaluation.NumericPrediction;
import weka.classifiers.functions.SMOreg;
import weka.classifiers.timeseries.WekaForecaster;
import weka.classifiers.timeseries.core.TSLagMaker;
import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instance;
import weka.experiment.InstanceQuery;

/**
 * Clase encargada de predecir los pasos de nodos por horas
 *
 * @author antares
 */
public class PrecidePasoNodo {

  private static Config _c = new Config();
  private static Debug _d = new Debug();
  private conectarFusionTables cFT = new conectarFusionTables();
  private final String TABLAID = _c.get("ft.PASOSPORHORAS.ID");
  private String fecha;
  private String nodo;
  
  public Instances pasos = null;
  WekaForecaster forecaster = null;

  public PrecidePasoNodo(String fecha, String nodo) {
    this.fecha = fecha;
    this.nodo = nodo;
    
    forecaster = new WekaForecaster();
    
  }

  void GenerarModeloPrediccion(){
    try {
      if(pasos == null){
        try {
          pasos = cargarDatos();
        } catch (ParseException ex) {
          Logger.getLogger(PrecidePasoNodo.class.getName()).log(Level.SEVERE, null, ex);
        }
      }
      
      //Defimimos el atributo que queremos predecir
         forecaster.setFieldsToForecast("Total");
         
         //Definimos el método de predicción a emplear. En este caso, regresión lineal porque 
         //en el artículo es el que mejor ha funcionado
         forecaster.setBaseForecaster(new SMOreg());
         
         //Defimimos el atributo que "marca" el tiempo y su peridiocidad
         forecaster.getTSLagMaker().setTimeStampField("Intervalo");
         forecaster.getTSLagMaker().setMinLag(1);
         forecaster.getTSLagMaker().setMaxLag(24);
         forecaster.getTSLagMaker().setPeriodicity(TSLagMaker.Periodicity.HOURLY);
         
         forecaster.buildForecaster(pasos, System.out);
      
      
    } catch (Exception ex) {
      Logger.getLogger(PrecidePasoNodo.class.getName()).log(Level.SEVERE, null, ex);
    }
    
  }
  
  Instances cargarDatos() throws ParseException {
    //Declaramos los atributos de las instancias
    Attribute a0 = new Attribute("Intervalo", "yyyy-MM-dd HH:mm:ss");
    Attribute a1 = new Attribute("Total");

    ArrayList<Attribute> c = new ArrayList<>();
    c.add(a0);
    c.add(a1);

    //Creamos el conjunto de instancias
    Instances instances = new Instances(nodo, c, 1000);

    //Instanciamos conexion con FT
    cFT = new conectarFusionTables();
    Sqlresponse r = cFT.select(TABLAID, "Intervalo, Total", "idNodo = " + nodo + " and ", "ORDER BY \'Intervalo\' DESC LIMIT 10000");

    for (List<Object> a : r.getRows()) {
      Instance i = new DenseInstance(2);

      String s0 = (String) a.get(0);
      String s1 = (String) a.get(1);

      //System.err.println(s0 + " ->" + s1);

      i.setValue(instances.attribute(0), instances.attribute(0).parseDate(s0));
      i.setValue(instances.attribute(1), Integer.parseInt(s1));

      instances.add(i);
    }

    instances.sort(0);


    return instances;
  }
  
  
  
  
  
}
