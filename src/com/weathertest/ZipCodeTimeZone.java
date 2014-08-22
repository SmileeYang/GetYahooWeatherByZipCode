package com.weathertest;

public class ZipCodeTimeZone {
private String mZipCode, mCity, mState, mTimeZone, mDst;
	
	public ZipCodeTimeZone(){
	}
	
	public ZipCodeTimeZone(String zipCode, String state, String city, String timezone, String dst) {
		mZipCode = zipCode;
		mState = state;
		mCity = city; 
		mTimeZone = timezone;
		mDst = dst;
	}
	
	public void setZipCode(String zipCode) {
		mZipCode = zipCode;
	}
	
	public String getZipCode() {
		return mZipCode;
	}
	
	public void setState(String state) {
		mState = state;
	}
	
	public String getState() {
		return mState;
	}
	
	public void setCity(String city) {
		mCity = city;
	}
	
	public String getCity() {
		return mCity;
	}
	
	public void setTimeZone(String timezone) {
		mTimeZone = timezone;
	}
	
	public String getTimeZone() {
		return mTimeZone;
	}
	
	public void setDst(String dst) {
		mDst = dst;
	}
	
	public String getDst() {
		return mDst;
	}
}
