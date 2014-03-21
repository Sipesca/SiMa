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

import java.io.*;

import java.util.List;
import weka.core.Instances;
import weka.classifiers.functions.GaussianProcesses;
import weka.classifiers.evaluation.NumericPrediction;
import weka.classifiers.timeseries.WekaForecaster;
import weka.classifiers.timeseries.core.TSLagMaker;






/**
 *
 * @author antares
 */
public class Prediccion extends Thread{

  @Override
  public void run() {
     try {
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
      List<List<NumericPrediction>> forecast = forecaster.forecast(12, System.out);

      
      
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

    } catch (Exception ex) {
      ex.printStackTrace();
    }
  }
  
  
  
}
