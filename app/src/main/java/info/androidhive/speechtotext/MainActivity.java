package info.androidhive.speechtotext;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Locale;
import java.util.concurrent.ExecutionException;
import java.util.regex.Pattern;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Dialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.media.audiofx.AcousticEchoCanceler;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.webkit.WebView;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class MainActivity extends Activity {

	private TextView txtSpeechInput;
	private ImageButton btnSpeak;
	private final int REQ_CODE_SPEECH_INPUT = 100;
	private final int REQ_CODE_ASK_NAME = 101;
	private final int REQ_CODE_RESP_NAME = 102;
    Dialog dia;
	private WebView imageView;
	TextToSpeech t1;
	String[] strArray={"0","1","2","3"};
	String TryName="";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		Context context = MainActivity.this;

		strArray[0] = getString(R.string.hello0);
		strArray[1] = getString(R.string.hello1);
		strArray[2] = getString(R.string.hello2);
		strArray[3] = getString(R.string.hello3);

		t1=new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {
			@Override
			public void onInit(int status) {
				if(status != TextToSpeech.ERROR) {
					t1.setLanguage(Locale.US);
				}
			}
		});

		txtSpeechInput = (TextView) findViewById(R.id.txtSpeechInput);
		btnSpeak = (ImageButton) findViewById(R.id.btnSpeak);

		dia = new Dialog(context, R.style.edit_AlertDialog_style);
		dia.setContentView(R.layout.activity_start_dialog);
		imageView = (WebView) dia.findViewById(R.id.CameraImage);
		imageView.setScrollbarFadingEnabled(false);
		imageView.setWebViewClient(new myWebClient(dia));//show dia after loading the page
		//Disable the horizontal scroll bar
		imageView.setHorizontalScrollBarEnabled(false);
		//Enable JavaScript
		imageView.getSettings().setJavaScriptEnabled(true);
		//Set the user agent
		imageView.getSettings().setUserAgentString("AndroidWebView");
		//Clear the cache
		imageView.clearCache(true);
		//Clear the cache
		// hide the action bar
		getActionBar().hide();

		btnSpeak.setOnClickListener(new View.OnClickListener() {

			@TargetApi(Build.VERSION_CODES.JELLY_BEAN)
			@Override
			public void onClick(View v) {
				if(AcousticEchoCanceler.isAvailable()) {
					Log.v("voice","avaliable");
				}
				else{
					Log.v("voice","not avaliable");
				}
				String hello = strArray[(int)(Math.random()*4)];
				tts(hello);
				promptSpeechInput(REQ_CODE_SPEECH_INPUT);
			}
		});

	}

	/**
	 * Showing google speech input dialog
	 * */
	private void tts(String str){
		txtSpeechInput.setText(str);
		t1.speak(str , TextToSpeech.QUEUE_FLUSH, null);
		while(t1.isSpeaking()){
			;
		}
	}
	private void promptSpeechInput(int ReqCode) {
		Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
		intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
				RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
		intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
		intent.putExtra(RecognizerIntent.EXTRA_PROMPT,
				getString(R.string.speech_prompt));
		try {
			startActivityForResult(intent, ReqCode);
		} catch (ActivityNotFoundException a) {
			Toast.makeText(getApplicationContext(),
					getString(R.string.speech_not_supported),
					Toast.LENGTH_SHORT).show();
		}

	}

	/**
	 * Receiving speech input
	 * */
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);

		switch (requestCode) {
		case REQ_CODE_SPEECH_INPUT: {
			if (resultCode == RESULT_OK && null != data) {
				ArrayList<String> result = data
						.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
				String res = result.get(0);

				if((Pattern.matches(".*light.*",res)||Pattern.matches(".*bulb.*",res))&&
						(Pattern.matches(".*on.*",res))){
					Log.e("Light","turn on");
					try {
						String resp = new GetDeviceResponse().execute("http://192.168.1.177:8000/yeelight/turn_on").get();
						Log.e("resp",resp);
						if(resp!=null) {
							txtSpeechInput.setText("Your light is on");
							t1.speak("Your light is on", TextToSpeech.QUEUE_FLUSH, null);
						}
					} catch (InterruptedException e) {
						e.printStackTrace();
					} catch (ExecutionException e) {
						e.printStackTrace();
					}

				}
				else if( (Pattern.matches(".*light.*",res)||Pattern.matches(".*bulb.*",res))&&
						(Pattern.matches(".*off.*",res))){
					Log.e("Light","turn off" +
							"");
					try {
						if(new GetDeviceResponse().execute("http://192.168.1.177:8000/yeelight/turn_off").get()!=null) {
							txtSpeechInput.setText("Your light is off");
							t1.speak("Your light is off", TextToSpeech.QUEUE_FLUSH, null);
						}
					} catch (InterruptedException e) {
						e.printStackTrace();
					} catch (ExecutionException e) {
						e.printStackTrace();
					}
				}
				else if((Pattern.matches(".*robot.*",res)||Pattern.matches(".*vacuum.*",res))&&
						(Pattern.matches(".*on.*",res)||Pattern.matches(".*start.*",res))){
					try {
						if(new GetDeviceResponse().execute("http://192.168.1.177:8000/xiaomi_vacuum/turn_on").get()!=null) {
							txtSpeechInput.setText("Your robot is on");
							t1.speak("Your robot is on", TextToSpeech.QUEUE_FLUSH, null);
						}
					} catch (InterruptedException e) {
						e.printStackTrace();
					} catch (ExecutionException e) {
						e.printStackTrace();
					}
				}
				else if((Pattern.matches(".*robot.*",res)||Pattern.matches(".*vacuum.*",res))&&
						(Pattern.matches(".*off.*",res)||Pattern.matches(".*stop.*",res))){
					try {
						if(new GetDeviceResponse().execute("http://192.168.1.177:8000/xiaomi_vacuum/turn_off").get()!=null) {
							txtSpeechInput.setText("Your robot is off");
							t1.speak("Your robot is off", TextToSpeech.QUEUE_FLUSH, null);
						}
					} catch (InterruptedException e) {
						e.printStackTrace();
					} catch (ExecutionException e) {
						e.printStackTrace();
					}
				}
				else if((Pattern.matches(".*change.*",res)||Pattern.matches(".*set.*",res))&&
						(Pattern.matches(".*color.*",res)||Pattern.matches(".*light.*",res))){
					String[] words = res.split(" ");
					String color = words[words.length-1];
					Log.e("Light",color);
					new GetDeviceResponse().execute("http://192.168.1.177:8000/yeelight/color_name?color_name="+color.toLowerCase());
					txtSpeechInput.setText(color+", here it is");
					t1.speak(color+",here it is", TextToSpeech.QUEUE_FLUSH, null);
				}

				else if((Pattern.matches(".*photo.*",res)||Pattern.matches(".*picture.*",res))&&
					(Pattern.matches(".*camera.*",res)||Pattern.matches(".*nest.*",res))){
					Log.v("camera","camera called");
					imageView.loadUrl("http://192.168.1.177:8000/nest_camera/snap_shot_response");
					txtSpeechInput.setText("Here is what I see");
					t1.speak("Here is what I see", TextToSpeech.QUEUE_FLUSH, null);
					//dia.show();
				}
				else if((Pattern.matches(".*detect.*",res)||Pattern.matches(".*analy.*",res))&&
						(Pattern.matches(".*camera.*",res)||Pattern.matches(".*nest.*",res)||Pattern.matches(".*face.*",res))){
					Log.v("camera","detect called");
                    imageView.loadUrl("http://192.168.1.177:8000/nest_camera/snap_shot_response");
					try {
						String faceResp = new GetDeviceResponse().execute("http://192.168.1.177:8000/nest_camera/face_detect_response").get();
						if(faceResp.equals("Please try again"))
							txtSpeechInput.setText("Please try again");
						JSONObject jsonObj = new JSONObject(faceResp);
						JSONObject json_data = new JSONObject(jsonObj.get("data").toString());
						JSONArray faces = new JSONArray(json_data.get("faces").toString());
						JSONObject face1 = new JSONObject(faces.get(0).toString());
						JSONObject attributes = new JSONObject(face1.get("attributes").toString());

						JSONObject ethnicity = new JSONObject(attributes.get("ethnicity").toString());
						txtSpeechInput.setText("ethnicity:"+ethnicity.get("value").toString());
						JSONObject age = new JSONObject(attributes.get("age").toString());
						txtSpeechInput.append("\nage:"+age.get("value").toString());
						JSONObject smile = new JSONObject(attributes.get("smile").toString());
						txtSpeechInput.append("\nsmile:"+smile.get("value").toString());
						JSONObject gender = new JSONObject(attributes.get("gender").toString());
						txtSpeechInput.append("\ngender:"+gender.get("value").toString());
					} catch (InterruptedException e) {
						e.printStackTrace();
					} catch (ExecutionException e) {
						e.printStackTrace();
					} catch (JSONException e) {
                        txtSpeechInput.setText("No face detected");
						e.printStackTrace();
					}
				}
				else if((Pattern.matches(".*remember.*",res)||Pattern.matches(".*keep.*",res))||Pattern.matches(".*create.*",res)	&&
						 (Pattern.matches(".*person.*",res)||Pattern.matches(".*people.*",res)||Pattern.matches(".*face.*",res))){
					Log.v("camera","saving called");
					String str = "what is the name";
					tts(str);
					txtSpeechInput.setText(str);
					promptSpeechInput(REQ_CODE_ASK_NAME);
				}
				else if((Pattern.matches(".*recognize.*",res)||Pattern.matches(".*name.*",res))||Pattern.matches(".*who is.*",res)	&&
						(Pattern.matches(".*person.*",res)||Pattern.matches(".*who .* is.*",res)||Pattern.matches(".*face.*",res))){
						String faceResp="";
					try {
						faceResp= new GetDeviceResponse().execute("http://192.168.1.177:8000/nest_camera/face_compare_response").get();
					} catch (InterruptedException e) {
						e.printStackTrace();
					} catch (ExecutionException e) {
						e.printStackTrace();
					}
					if(!faceResp.equals("\"\"")){
						Log.v("camera","compare called name:"+faceResp);
						String str = faceResp.substring(1,faceResp.length()-1);
						tts(str);
						txtSpeechInput.setText(str);
					}else{
						Log.v("camera","no face");
						String str = "I didn't see any person";
						tts(str);
						txtSpeechInput.setText(str);
				}
				}
				else if((Pattern.matches(".*tv.*",res)||Pattern.matches(".*TV.*",res))&&
						(Pattern.matches(".*on.*",res))){
					Log.v("camera","TV  called");
					try {
						new GetDeviceResponse().execute("http://192.168.1.177:8000/harmony/turn_on").get();
					} catch (InterruptedException e) {
						e.printStackTrace();
					} catch (ExecutionException e) {
						e.printStackTrace();
					}
					txtSpeechInput.setText("Your TV is on");
					t1.speak("Your TV is on", TextToSpeech.QUEUE_FLUSH, null);
				}
				else if((Pattern.matches(".*tv.*",res)||Pattern.matches(".*TV.*",res))&&
						(Pattern.matches(".*off.*",res))){
					Log.v("camera","TV  called");
					try {
						new GetDeviceResponse().execute("http://192.168.1.177:8000/harmony/turn_off").get();
					} catch (InterruptedException e) {
						e.printStackTrace();
					} catch (ExecutionException e) {
						e.printStackTrace();
					}
					txtSpeechInput.setText("Your TV is off");
					t1.speak("Your TV is off", TextToSpeech.QUEUE_FLUSH, null);
				}
				else txtSpeechInput.setText(res);
			}
			break;
		}
		case REQ_CODE_ASK_NAME: {
			if (resultCode == RESULT_OK && null != data) {
				ArrayList<String> result = data
						.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
				TryName = result.get(0);
				String str =TryName+", is that correct?";
				txtSpeechInput.setText(str);
				tts(str);
				promptSpeechInput(REQ_CODE_RESP_NAME);
			}
		}
		case REQ_CODE_RESP_NAME: {
				if (resultCode == RESULT_OK && null != data) {
					ArrayList<String> result = data
							.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
					String res = result.get(0);
					if((Pattern.matches(".*yes.*",res)||Pattern.matches(".*correct.*",res))){
						String str = TryName+"'s Face data saved";
						tts(str);
						txtSpeechInput.setText(str);
                        imageView.loadUrl("http://192.168.1.177:8000/nest_camera/snap_shot_response");
						new GetDeviceResponse().execute("http://192.168.1.177:8000/nest_camera/snap_shot_image?name="+TryName);
					}
					if((Pattern.matches(".*no.*",res)||Pattern.matches(".*not.*",res))){
						String str = "oops, lets try again, what is the name";
						tts(str);
						txtSpeechInput.setText(str);
						promptSpeechInput(REQ_CODE_ASK_NAME);
					}
				}
		}


		}
	}


	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}
	public class GetDeviceResponse extends AsyncTask<String, Void, String> {
		@Override
		protected String doInBackground(String... urls) {
			String searchUrl = urls[0];
			String Response = "invalid";
			try {
				Response=GetDeviceResponse(searchUrl);
			}  catch (IOException e){
				e.printStackTrace();
			}
			return Response;
		}
		public String GetDeviceResponse(String urlstring) throws IOException {
			StringBuilder buf = new StringBuilder(urlstring);
			URL url = new URL(buf.toString());
			HttpClient httpclient = new DefaultHttpClient();
			HttpGet httpget = new HttpGet(url.toString());
			HttpResponse response = httpclient.execute(httpget);
			HttpEntity entity = response.getEntity();
			return EntityUtils.toString(entity);
		}
		@Override
		protected void onPostExecute(String s) {
			if(s!=null && !s.equals("")){
				;
			}
			super.onPostExecute(s);
		}
	}

}



