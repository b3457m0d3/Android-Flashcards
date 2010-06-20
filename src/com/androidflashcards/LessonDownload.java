package com.androidflashcards;

import com.androidflashcards.filters.LessonFilter;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.app.AlertDialog;
import android.content.DialogInterface;

import java.io.BufferedInputStream;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.net.URL;
import java.net.URLConnection;

public class LessonDownload extends Activity implements Runnable {
	
	private String lname,ldesc,lurl,lfilt,ltarget,lenc;
	private LessonDownload me;
	private ProgressDialog pd;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.lesson_download);
		Bundle extras = getIntent().getExtras();
		lname = extras.getString("LessonName");
		ldesc = extras.getString("LessonDesc");
		lurl = extras.getString("LessonUrl");
		lfilt = extras.getString("LessonFilter");
		ltarget = extras.getString("LessonTarget");
		lenc = extras.getString("LessonEncoding");
		me = this;
		setResult(0);

		final TextView nametv = (TextView) findViewById(R.id.lesson_name_tv);
		nametv.setText(lname);
		final TextView desctv = (TextView) findViewById(R.id.lesson_desc_tv);
		desctv.setText(ldesc);
		
		final Button cancel_button = (Button) findViewById(R.id.cancel_button);
		cancel_button.setOnClickListener(new View.OnClickListener() {
				public void onClick(View v) {
					finish();
				}
			});

		final Button dl_button = (Button) findViewById(R.id.download_button);
		dl_button.setOnClickListener(new View.OnClickListener() {
				public void onClick(View v) {
					pd = ProgressDialog.show(me, "", 
																	 "Downloading lesson...", 
																	 true);
					Thread t = new Thread(me);
					t.start();
				}
			});
	}

	public void run() {
		try {
			doDownload(lurl,lfilt,ltarget,lenc);
			handler.sendEmptyMessage(0);
		} catch (Exception e) {
			e.printStackTrace();
			handler.sendMessage(handler.obtainMessage(1,e.getMessage()));
		}
	}
	
	private Handler handler = new Handler() {
			@Override
			public void handleMessage(Message msg) {
				pd.dismiss();
				if (msg.what == 0) {
					AlertDialog alertDialog = new AlertDialog.Builder(me).create();
					alertDialog.setTitle("Success");
					alertDialog.setMessage("Lesson downloaded successfully.\n\nPress OK to go back to the list of available lessons.\n\n(You can press the back button from the list to go back to the lesson select screen)");
					alertDialog.setButton("OK", new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int which) {
								setResult(1);
								finish();
								return;
							} });
					alertDialog.show();
				} else if (msg.what == 1) {
					AlertDialog alertDialog = new AlertDialog.Builder(me).create();
					alertDialog.setTitle("Error");
					alertDialog.setMessage("Sorry, but an error occured trying to download this lesson:\n\n"+msg.obj);
					alertDialog.setButton("OK", new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int which) {
								setResult(0);
								finish();
								return;
							} });
					alertDialog.show();
				}
			}
		};

	private void doDownload(String _url, String filtClass, String target, String enc) throws Exception {
		URL url = new URL(_url);
		String fdest = null;
		if (target != null) 
			fdest = "/sdcard/flashcards/"+target;
		else {
			fdest = url.getFile();
			fdest = "/sdcard/flashcards/"+fdest.substring(fdest.lastIndexOf('/')+1);
		}
		URLConnection conn = url.openConnection();
		if (filtClass == null) {
			BufferedInputStream bis = new BufferedInputStream(conn.getInputStream());
			BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(fdest));
			int i;
			while ((i = bis.read()) != -1)
				bos.write(i);
			bos.close();
			bis.close();
		} else {
			BufferedReader br = new BufferedReader
				(enc == null?
				 new InputStreamReader(conn.getInputStream()):
				 new InputStreamReader(conn.getInputStream(),enc));
			BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(fdest));
			String cline;
			LessonFilter filt = (LessonFilter)Class.forName(filtClass).newInstance();
			int lnum = 0;
			while ((cline = br.readLine())!=null) {
				String nl = filt.filterLine(cline,++lnum);
				if (nl != null)
					bos.write(nl.getBytes());
			}
			bos.close();
			br.close();
		}
	}

}