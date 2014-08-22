package com.weathertest;

public class WeatherInfo {
private String mTemperature, mHumidity, mCodition;
	
	public WeatherInfo(){
	}
	
	public WeatherInfo(String temperature, String humidity, String codition) {
		mTemperature = temperature;
		mHumidity = humidity;
		mCodition = codition; 
	}
	
	public void setTemperature(String temperature) {
		mTemperature = temperature;
	}
	
	public String getTemperature() {
		return mTemperature;
	}
	
	public void setHumidity(String humidity) {
		mHumidity = humidity;
	}
	
	public String getHumidity() {
		return mHumidity;
	}
	
	public void setCodition(String codition) {
		mCodition = codition;
	}
	
	public String getCodition() {
		return mCodition;
	}
}
