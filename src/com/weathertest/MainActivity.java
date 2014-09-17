package com.weathertest;

import android.os.Bundle;
import android.util.Log;
import android.view.View;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.http.HttpEntity;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import android.net.Uri;
import android.os.AsyncTask;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

public class MainActivity extends Activity {

	final String yahooPlaceApisBase = "http://query.yahooapis.com/v1/public/yql?q=select*from%20geo.places%20where%20text=";
	final String yahooapisFormat = "&format=xml";
	String yahooWoeidAPIsQuery;

	private final String TAG = getClass().getSimpleName();

	private String mZipCode, mState, mCity, mLatitude, mLongitude, mTimezone,
			mDst, mTemperature, mHumidity, mCondition, mCurrentTyping = null,
			searchWoeidResult, weatherString, weatherResult, weatherIconUrl;
	private long mLastTimeStamp, mCurrTimeStamp, mTimeGap,
			mFourHours = 14400000;
	private boolean isRecorded = false, isSearchSuccess = false;
	ZipCodeDBHelper mZipCodeDBHelper = null;

	private EditText mTypingZip;
	private Button search;
	private TextView weatherInfoText;
	private ImageView weatherIcon;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		mZipCodeDBHelper = new ZipCodeDBHelper(this);

		mTypingZip = (EditText) findViewById(R.id.zip);
		search = (Button) findViewById(R.id.search);
		search.setOnClickListener(searchOnClickListener);
		weatherInfoText = (TextView) findViewById(R.id.weather);
		weatherIcon = (ImageView) findViewById(R.id.weather_icon);
	}

	@Override
	public void onResume() {
		mTypingZip.setText("");
		isSearchSuccess = false;
		Log.d(TAG, "onResume, isRecorded=" + isRecorded + ", isSearchSuccess=" + isSearchSuccess);
		super.onResume();
	}

	public void onPause() {
		super.onPause();
	}

	Button.OnClickListener searchOnClickListener = new Button.OnClickListener() {

		@Override
		public void onClick(View arg0) {
			mZipCode = mTypingZip.getText().toString();
			Log.d(TAG, "mZipCode=" + mZipCode + ", mCurrentTyping=" + mCurrentTyping);
			while (mZipCode.length() > 2 && mZipCode.length() < 5) {
				String isThree = "00";
				String isFour = "0";
				if (mZipCode.length() == 3) {
					mZipCode = isThree + mZipCode;
					Log.d(TAG, "Now mZipCode=" + mZipCode);
				} else if (mZipCode.length() == 4) {
					mZipCode = isFour + mZipCode;
					Log.d(TAG, "Now mZipCode=" + mZipCode);
				}
			}
			if (mZipCode.equals("")) {
				Toast.makeText(getBaseContext(), "Please enter zip code!", Toast.LENGTH_SHORT).show();
			} else if (mZipCode.length() < 3) {
				Toast.makeText(getBaseContext(), "Your zip code is too short!", Toast.LENGTH_SHORT).show();
			} else {
				ArrayList<ZipCodeTimeZone> locationList = mZipCodeDBHelper.getZipData(MainActivity.this, mZipCode);
				Log.d(TAG, "getZipCodeData()");
				if (locationList != null && !locationList.isEmpty()) {
					mCurrTimeStamp = System.currentTimeMillis();
					isSearchSuccess = true;
					mTimeGap = mCurrTimeStamp - mLastTimeStamp;
					Log.d(TAG, ">>>>>mTimeGap=" + mTimeGap);
					if (!isRecorded || !mCurrentTyping.equalsIgnoreCase(mZipCode)) {
						mCurrentTyping = mZipCode;
						isRecorded = true;
						mLastTimeStamp = mCurrTimeStamp;
						Log.d(TAG, "---mCurrentType=" + mCurrentTyping + ", mLastTimeStamp=" + mLastTimeStamp);
						for (ZipCodeTimeZone zip : locationList) {
							mZipCode = zip.getZipCode();
							mState = zip.getState();
							mCity = zip.getCity();
							mTimezone = zip.getTimeZone();
							mDst = zip.getDst();
							Log.d(TAG, "getZipCode()=" + zip.getZipCode()
									+ " getState()=" + zip.getState()
									+ " getCity()=" + zip.getCity()
									+ " getTimezone()=" + zip.getTimeZone()
									+ " getDst()=" + zip.getDst());
						}
						Log.d(TAG, "MyQueryYahooPlaceTask().execute()");
						new MyQueryYahooPlaceTask().execute();
					} else if (isRecorded && mCurrentTyping.equalsIgnoreCase(mZipCode) && (mTimeGap <= mFourHours)) {
						Log.d(TAG, "---equals---mTimeGap=" + mTimeGap);
						Log.d(TAG, "mCity=" + mCity + " mTemperature=" + mTemperature + " mHumidity=" + mHumidity + " mCondition=" + mCondition);
					}
				} else {
					Toast.makeText(MainActivity.this, "No such zip code, please try again!", Toast.LENGTH_SHORT).show();
					mTypingZip.setText("");
				}
			}
		}
	};

	private class MyQueryYahooPlaceTask extends AsyncTask<Void, Void, Bitmap> {

		@Override
		protected Bitmap doInBackground(Void... arg0) {
			// If want to search weather, need to do: 
			// QueryYahooWoeidAPIs(uriPlace), QueryYahooWeather(), convertStringToDocument(weatherString), getWeatherInfo(weatherDoc)
			String uriPlace = Uri.encode(mCity + " " + mState);
			searchWoeidResult = queryYahooWoeidAPIs(uriPlace);
			weatherString = queryYahooWeather();
			Document woeidDoc = convertStringToDocument(weatherString);
			if (woeidDoc != null) {
				ArrayList<WeatherInfo> resultList = getWeatherInfo(woeidDoc);
				if (resultList != null) {
					for (WeatherInfo wi : resultList) {
						mTemperature = wi.getTemperature();
						mHumidity = wi.getHumidity();
						mCondition = wi.getCodition();
					}
					weatherResult = "city: " + mCity + "\n" + "state: " + mState + "\n\n"
							+ "temperature: " + mTemperature + "°F\n" 
							+ "humidity: " + mHumidity + "%" + "\n" 
							+ "Condition: " + mCondition + "\n";
					Log.d(TAG, weatherResult);
				} else {
					Toast.makeText(MainActivity.this, "Wrong", Toast.LENGTH_SHORT).show();
					Log.d(TAG, "Wrong");
				}
			} else {
				Log.d(TAG, "Cannot convert String To Document!");
			}
			try { // get abc in <img src="abc">
				String imgRegex = "<img[^>]"; // http://l.yimg.com/a/i/us/we/52/33.gif"/><br />
				String[] tokens = weatherString.split(imgRegex);
				for (int i = 0; i < tokens.length; i++) {
					int a = tokens[i].indexOf("src=\"");
					int b = tokens[i].indexOf("\"/>"); // http://l.yimg.com/a/i/us/we/52/33.gif
					weatherIconUrl = tokens[i].substring(a + 5, b);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
			return loadingImg(weatherIconUrl);
		}

		@SuppressLint("NewApi")
		@Override
		protected void onPostExecute(Bitmap bitmap) {
			Log.d(TAG, "onPostExecute");
			weatherInfoText.setText(weatherResult);
			weatherIcon.setImageBitmap(bitmap);
			super.onPostExecute(bitmap);
		}

	}

	private String queryYahooWoeidAPIs(String uriPlace) {
		yahooWoeidAPIsQuery = yahooPlaceApisBase + "%22" + uriPlace + "%22" + yahooapisFormat;
		String woeidString = queryYahooWeather(yahooWoeidAPIsQuery);
		Document woeidDoc = convertStringToDocument(woeidString);
		return parseWoeid(woeidDoc);
	}

	private String parseWoeid(Document woeidDoc) {
		String woeidResult = "";
		NodeList nodeListDescription = woeidDoc.getElementsByTagName("woeid"); // finding <woeid>
		if (nodeListDescription.getLength() >= 0) {
			for (int i = 0; i < nodeListDescription.getLength(); i++) {
				woeidResult = (nodeListDescription.item(i).getTextContent());
			}
		} else {
			return woeidResult;
		}
		return woeidResult;
	}

	private Document convertStringToDocument(String woeidString) {
		Document woeidDoc = null;
		DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder parser;
		try {
			parser = dbFactory.newDocumentBuilder();
			woeidDoc = parser.parse(new ByteArrayInputStream(woeidString.getBytes()));
		} catch (ParserConfigurationException e1) {
			e1.printStackTrace();
		} catch (SAXException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return woeidDoc;
	}

	private String queryYahooWeather(String woeidResult) {
		String queryResult = "";
		HttpClient httpClient = new DefaultHttpClient();
		HttpGet httpGet = new HttpGet(woeidResult);
		try {
			HttpEntity httpEntity = httpClient.execute(httpGet).getEntity();
			if (httpEntity != null) {
				InputStream inputStream = httpEntity.getContent();
				Reader reader = new InputStreamReader(inputStream);
				BufferedReader bufferedReader = new BufferedReader(reader);
				StringBuilder stringBuilder = new StringBuilder();
				String stringReadLine = null;
				while ((stringReadLine = bufferedReader.readLine()) != null) {
					stringBuilder.append(stringReadLine + "\n");
				}
				queryResult = stringBuilder.toString();
			}
		} catch (ClientProtocolException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return queryResult;
	}

	private String queryYahooWeather() {
		String qResult = "";
		String queryString = "http://weather.yahooapis.com/forecastrss?w=" + searchWoeidResult + "&u=f";
		HttpClient httpClient = new DefaultHttpClient();
		HttpGet httpGet = new HttpGet(queryString);
		try {
			HttpEntity httpEntity = httpClient.execute(httpGet).getEntity();
			if (httpEntity != null) {
				InputStream inputStream = httpEntity.getContent();
				Reader in = new InputStreamReader(inputStream);
				BufferedReader bufferedreader = new BufferedReader(in);
				StringBuilder stringBuilder = new StringBuilder();
				String stringReadLine = null;
				while ((stringReadLine = bufferedreader.readLine()) != null) {
					stringBuilder.append(stringReadLine + "\n");
				}
				qResult = stringBuilder.toString();
			}
		} catch (ClientProtocolException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return qResult;
	}

	private ArrayList<WeatherInfo> getWeatherInfo(Document srcDoc) {
		ArrayList<WeatherInfo> resultList = new ArrayList<WeatherInfo>();
		WeatherInfo wi = new WeatherInfo();
		NodeList conditionNodeList = srcDoc.getElementsByTagName("yweather:condition");
		if (conditionNodeList != null && conditionNodeList.getLength() > 0) {
			Node conditionNode = conditionNodeList.item(0);
			NamedNodeMap conditionNamedNodeMap = conditionNode.getAttributes();
			wi.setCodition(conditionNamedNodeMap.getNamedItem("text").getNodeValue().toString());
			wi.setTemperature(conditionNamedNodeMap.getNamedItem("temp").getNodeValue());
		} else {
			mCondition = "EMPTY";
			mTemperature = "EMPTY";
		}
		NodeList humidityNodeList = srcDoc.getElementsByTagName("yweather:atmosphere");
		if (humidityNodeList != null && humidityNodeList.getLength() > 0) {
			Node humidityNode = humidityNodeList.item(0);
			NamedNodeMap humidityNamedNodeMap = humidityNode.getAttributes();
			wi.setHumidity(humidityNamedNodeMap.getNamedItem("humidity").getNodeValue().toString());
		} else {
			mHumidity = "EMPTY";
		}
		resultList.add(wi);
		return resultList;
	}
	
	private Bitmap loadingImg(String weatherIconUrl) {
		Bitmap bitmap = null;
		try {
			Log.d(TAG, "weatherIcon set");
			bitmap = BitmapFactory.decodeStream((InputStream) new URL(weatherIconUrl).getContent());
		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return bitmap;
	}
}
