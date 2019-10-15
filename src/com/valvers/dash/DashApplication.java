package com.valvers.dash;

import android.app.Application;

public class DashApplication extends Application {
	private int rpm = 0;
	private int battery = 0;
	private int kph = 0;
	private int fuel_p = 0;
	private int oil_p = 0;
	private int air_temp = 0;
	private int oil_temp = 0;
	private int water_temp = 0;
	
	public synchronized int getRpm()
	{
		return rpm;
	}
	
	public synchronized void setRpm(int r)
	{
		rpm = r;
	}

	public synchronized int getBattery()
	{
		return battery;
	}
	
	public synchronized void setBattery(int r)
	{
		battery = r;
	}

	public synchronized int getKph()
	{
		return kph;
	}
	
	public synchronized void setKph(int r)
	{
		kph = r;
	}

	public synchronized int getFuelPressure()
	{
		return fuel_p;
	}
	
	public synchronized void setFuelPressure(int r)
	{
		fuel_p = r;
	}

	public synchronized int getOilPressure()
	{
		return oil_p;
	}
	
	public synchronized void setOilPressure(int r)
	{
		oil_p = r;
	}

	public synchronized int getAirTemp()
	{
		return air_temp;
	}
	
	public synchronized void setAirTemp(int r)
	{
		air_temp = r;
	}

	public synchronized int getOilTemp()
	{
		return oil_temp;
	}
	
	public synchronized void setOilTemp(int r)
	{
		oil_temp = r;
	}

	public synchronized int getWaterTemp()
	{
		return water_temp;
	}
	
	public synchronized void setWaterTemp(int r)
	{
		water_temp = r;
	}

	
}
