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
 *
 * @author antares
 */



public class Prediccion extends Thread{

  private Config _c = new Config();
  private Debug _d = new Debug();
  private conectarFusionTables cFT = new conectarFusionTables();
  private String fecha;
  private final String TABLAID = _c.get("ft.PASOSPORHORAS.ID");
  
  
  
  
  
  InstanceQuery query = null;
  String nodo = null;

  public Prediccion(String elnodo) {
    nodo = elnodo;
  }
  
  Instances cargarDatos() throws ParseException{
    //Declaramos los atributos de las instancias
    Attribute a0 = new Attribute("Intervalo", "yyyy-MM-dd HH:mm:ss");
    Attribute a1 = new Attribute("Total");
    
    ArrayList<Attribute> c = new ArrayList<>();
    c.add(a0); c.add(a1);
    
    //Creamos el conjunto de instancias
    Instances instances = new Instances (nodo, c ,1000);
    
    //Instanciamos conexion con FT
    cFT = new conectarFusionTables();
    Sqlresponse r = cFT.select(TABLAID,"Intervalo, Total","idNodo = " +nodo+" and ","ORDER BY \'Intervalo\' DESC LIMIT 10000");
    
    for(List<Object> a : r.getRows()){
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
  
  
  @Override
  public void run() {
     try {
       
       Instances pasos = cargarDatos();
       
       System.err.println(pasos.size());
       
       //Instanciamos el predictor
       WekaForecaster forecaster = new WekaForecaster();
       
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
       
       System.err.printf("Terminó");
       
       
      forecaster.primeForecaster(pasos);
       
       List<List<NumericPrediction>> forecast = forecaster.forecast(24, System.out);

      
      
      // output the predictions. Outer list is over the steps; inner list is over
      // the targets
      for (int i = 0; i < 24; i++) {
        List<NumericPrediction> predsAtStep = forecast.get(i);
        for (int j = 0; j < 1; j++) {
          NumericPrediction predForTarget = predsAtStep.get(j);
          System.out.print("" + predForTarget.predicted() + " ");
        }
        System.out.println();
      }
       
   /*    
       
      // path to the Australian wine data included with the time series forecasting
      // package
      String pathToWineData = weka.core.WekaPackageManager.PACKAGES_DIR.toString()
        + File.separator + "timeseriesForecasting" + File.separator + "sample-data"
        + File.separator + "wine.arff";

      // load the wine data
      Instances wine = new Instances(new BufferedReader(new FileReader(pathToWineData)));      
      
      // new forecaster
      WekaForecaster forecaster = new WekaForecaster();

      // set the targets we want to forecast. This method calls
      // setFieldsToLag() on the lag maker object for us
      forecaster.setFieldsToForecast("Fortified,Dry-white");

      // default underlying classifier is SMOreg (SVM) - we'll use
      // gaussian processes for regression instead
      forecaster.setBaseForecaster(new GaussianProcesses());

      forecaster.getTSLagMaker().setTimeStampField("Date"); // date time stamp
      forecaster.getTSLagMaker().setMinLag(1);
      forecaster.getTSLagMaker().setMaxLag(12); // monthly data

      // add a month of the year indicator field
      forecaster.getTSLagMaker().setAddMonthOfYear(true);

      // add a quarter of the year indicator field
      forecaster.getTSLagMaker().setAddQuarterOfYear(true);

      // build the model
      forecaster.buildForecaster(wine, System.out);

      // prime the forecaster with enough recent historical data
      // to cover up to the maximum lag. In our case, we could just supply
      // the 12 most recent historical instances, as this covers our maximum
      // lag period
      forecaster.primeForecaster(wine);

      // forecast for 12 units (months) beyond the end of the
      // training data
      <<List<List<NumericPrediction>> forecast = forecaster.forecast(12, System.out);

      
      
      // output the predictions. Outer list is over the steps; inner list is over
      // the targets
      for (int i = 0; i < 12; i++) {
        List<NumericPrediction> predsAtStep = forecast.get(i);
        for (int j = 0; j < 2; j++) {
          NumericPrediction predForTarget = predsAtStep.get(j);
          System.out.print("" + predForTarget.predicted() + " ");
        }
        System.out.println();
      }

      // we can continue to use the trained forecaster for further forecasting
      // by priming with the most recent historical data (as it becomes available).
      // At some stage it becomes prudent to re-build the model using current
      // historical data.
*/
    } catch (Exception ex) {
      ex.printStackTrace();
    }


  }
  
  
}
