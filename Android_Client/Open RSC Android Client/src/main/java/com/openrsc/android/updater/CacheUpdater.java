package com.openrsc.android.updater;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.openrsc.client.R;
import com.openrsc.client.android.GameActivity;
import orsc.osConfig;
import orsc.util.GenUtil;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

public class CacheUpdater extends Activity {

    private TextProgressBar progressBar;

    private TextView tv1;
    private boolean completed = false;

    List<String> excludedFiles = new ArrayList<>();
    List<String> refuseUpdate = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.updater);
        progressBar = findViewById(R.id.progressBar);
        progressBar.setTextSize(18);
        progressBar.setIndeterminate(false);
        progressBar.setMax(100);

        Button launchButton = findViewById(R.id.launch_client);
        launchButton.setVisibility(View.GONE);
        launchButton.setOnClickListener(v -> {
            if (completed) {
                Intent mainIntent = new Intent(CacheUpdater.this, GameActivity.class);
                mainIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(mainIntent);
                finish();
            }
        });
        //Start Game
        tv1 = findViewById(R.id.textView1);
        setStatus("Checking for game-cache updates...");
        Handler handler = new Handler();
        handler.post(() -> new UpdateTask().execute());
    }

    public void setStatus(String s) {
        tv1.setText(s);
    }

    @SuppressLint("StaticFieldLeak")
    class UpdateTask extends AsyncTask<String, String, String> {

        @Override
        protected String doInBackground(String... aurl) {
			excludedFiles.add(osConfig.MD5_TABLENAME);
			refuseUpdate.add("config.txt");

			File cacheHome = getFilesDir();
			if (!cacheHome.exists())
				cacheHome.mkdirs();

			File md5Table = new File(cacheHome, osConfig.MD5_TABLENAME);

			if (md5Table.exists()) {
				md5Table.delete();
			}

			downloadFile(md5Table, getFilesDir().toString() + File.separator);

			md5 localCache = new md5(md5Table.getParentFile(), "");
			md5 remoteCache = new md5(md5Table, "");

			for (md5.Entry entry : remoteCache.entries) {
				if (excludedFiles.contains(entry.getRef().getName()))
					continue;

				File entryFile = new File(cacheHome, entry.getRef().toString());
				entryFile.getParentFile().mkdirs();

				String localSum = localCache.getRefSum(entryFile);
				if (localSum != null) {
					if (refuseUpdate.contains(entry.getRef().getName()) ||
						localSum.equalsIgnoreCase(entry.getSum())) {
						continue;
					}
				}

				downloadFile(entryFile, getFilesDir().toString() + File.separator);
			}

            publishProgress("Updating completed...");
            return null;
        }

        @Override
        protected void onPostExecute(String result) {
            completed = true;
            showGameSelectionDialog();
        }

        void showGameSelectionDialog() {

            String desiredPath = "/storage/emulated/0/Android/data/user/0/com.openrsc.client/files" + File.separator;
            final AtomicReference<String> realPath = new AtomicReference<String>();
            FileOutputStream fos = null;

            try {
                fos = new FileOutputStream(desiredPath + "test.txt");
                realPath.set(desiredPath);
            } catch (Exception e) {
                try {
                    fos = new FileOutputStream(getFilesDir().getPath() + File.separator + "test.txt");
                    realPath.set(getFilesDir().getPath() + File.separator);
                } catch (Exception e2) {
                    try {
                        fos = new FileOutputStream(Objects.requireNonNull(getExternalFilesDir(null)).getPath() + File.separator + "test.txt");
                        realPath.set(Objects.requireNonNull(getExternalFilesDir(null)).getPath() + File.separator);
                    } catch (Exception ignored) {
                    }
                }
            } finally {
                GenUtil.close(fos);
            }

            System.out.println(" ");
            System.out.println(" ");
            System.out.println("Please select which game you wish to play.");
            System.out.println(" ");
            System.out.println("43594 openrsc / 43595 cabbage / 43596 preservation / 43597 openpk / 43598 wk / 43599 dev");
            System.out.println(" ");

            // setup the alert builder
            AlertDialog.Builder builder = new AlertDialog.Builder(CacheUpdater.this);
            builder.setTitle("Game Selection");

            // add a list
            String[] games = {"RSC Cabbage", "Open RSC", "RSC Preservation (alpha testing)", "Open PK (alpha testing)", "Dev Testing", "Local Instance"};
            builder.setItems(games, (dialog, which) -> {
                switch (which) {
                    case 0:
                        String ip_cabbage = "androidcheck.openrsc.com";
                        String port_cabbage = "43595";
                        FileOutputStream fileout_cabbage;

                        String pack = "Menus:1";
                        FileOutputStream fileout_cabbage2;
                        try {
                            fileout_cabbage = new FileOutputStream(realPath.get() + "ip.txt");
                            OutputStreamWriter outputWriter = new OutputStreamWriter(fileout_cabbage);
                            outputWriter.write(ip_cabbage);
                            outputWriter.close();

                            fileout_cabbage2 = new FileOutputStream(realPath.get() + "config.txt");
                            OutputStreamWriter outputWriter2 = new OutputStreamWriter(fileout_cabbage2);
                            outputWriter2.write(pack);
                            outputWriter2.close();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        try {
                            fileout_cabbage = new FileOutputStream(realPath.get() + "port.txt");
                            OutputStreamWriter outputWriter = new OutputStreamWriter(fileout_cabbage);
                            outputWriter.write(port_cabbage);
                            outputWriter.close();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        Intent mainIntent_cabbage = new Intent(CacheUpdater.this, GameActivity.class);
                        mainIntent_cabbage.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(mainIntent_cabbage);
                        finish();
                        return;
                    case 1:
                        String ip_openrsc = "androidcheck.openrsc.com";
                        String port_openrsc = "43594";
                        FileOutputStream fileout_openrsc;
                        try {
                            fileout_openrsc = new FileOutputStream(realPath.get() + "ip.txt");
                            OutputStreamWriter outputWriter = new OutputStreamWriter(fileout_openrsc);
                            outputWriter.write(ip_openrsc);
                            outputWriter.close();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        try {
                            fileout_openrsc = new FileOutputStream(realPath.get() + "port.txt");
                            OutputStreamWriter outputWriter = new OutputStreamWriter(fileout_openrsc);
                            outputWriter.write(port_openrsc);
                            outputWriter.close();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        Intent mainIntent_openrsc = new Intent(CacheUpdater.this, GameActivity.class);
                        mainIntent_openrsc.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(mainIntent_openrsc);
                        finish();
                        return;
                    case 2:
                        String ip_preservation = "androidcheck.openrsc.com";
                        String port_preservation = "43596";
                        FileOutputStream fileout_preservation;
                        try {
                            fileout_preservation = new FileOutputStream(realPath.get() + "ip.txt");
                            OutputStreamWriter outputWriter = new OutputStreamWriter(fileout_preservation);
                            outputWriter.write(ip_preservation);
                            outputWriter.close();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        try {
                            fileout_preservation = new FileOutputStream(realPath.get() + "port.txt");
                            OutputStreamWriter outputWriter = new OutputStreamWriter(fileout_preservation);
                            outputWriter.write(port_preservation);
                            outputWriter.close();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        Intent mainIntent_preservation = new Intent(CacheUpdater.this, GameActivity.class);
                        mainIntent_preservation.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(mainIntent_preservation);
                        finish();
                        return;
                    case 3:
                        String ip_openpk = "androidcheck.openrsc.com";
                        String port_openpk = "43597";
                        FileOutputStream fileout_openpk;
                        try {
                            fileout_openpk = new FileOutputStream(realPath.get() + "ip.txt");
                            OutputStreamWriter outputWriter = new OutputStreamWriter(fileout_openpk);
                            outputWriter.write(ip_openpk);
                            outputWriter.close();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        try {
                            fileout_openpk = new FileOutputStream(realPath.get() + "port.txt");
                            OutputStreamWriter outputWriter = new OutputStreamWriter(fileout_openpk);
                            outputWriter.write(port_openpk);
                            outputWriter.close();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        Intent mainIntent_openpk = new Intent(CacheUpdater.this, GameActivity.class);
                        mainIntent_openpk.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(mainIntent_openpk);
                        finish();
                        return;
                    case 4:
                        String ip_dev = "androidcheck.openrsc.com";
                        String port_dev = "43599";
                        FileOutputStream fileout_dev;
                        try {
                            fileout_dev = new FileOutputStream(realPath.get() + "ip.txt");
                            OutputStreamWriter outputWriter = new OutputStreamWriter(fileout_dev);
                            outputWriter.write(ip_dev);
                            outputWriter.close();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        try {
                            fileout_dev = new FileOutputStream(realPath.get() + "port.txt");
                            OutputStreamWriter outputWriter = new OutputStreamWriter(fileout_dev);
                            outputWriter.write(port_dev);
                            outputWriter.close();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        Intent mainIntent_dev = new Intent(CacheUpdater.this, GameActivity.class);
                        mainIntent_dev.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(mainIntent_dev);
                        finish();
                        return;
					case 5:
						LinearLayout layout = new LinearLayout(CacheUpdater.this);

						// TextView to enter ip
						final EditText ipBox = new EditText(CacheUpdater.this);
						ipBox.setHint("127.0.0.1");
						layout.addView(ipBox);

						// TextView to enter port
						final EditText portBox = new EditText(CacheUpdater.this);
						portBox.setHint("43594");
						layout.addView(portBox);

						new AlertDialog.Builder(CacheUpdater.this)
							.setTitle("Local Instance")
							.setMessage("Enter details for local instance")
							.setView(layout)
							.setPositiveButton("Enter", new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog, int whichButton) {
									String ip_local = "127.0.0.1";
									String port_local = "43594";

									if (!ipBox.getText().toString().trim().equals("")) {
										ip_local = ipBox.getText().toString().trim();
									}
									if (!portBox.getText().toString().trim().equals("")) {
										port_local = portBox.getText().toString().trim();
									}

									FileOutputStream fileout_local;
									try {
										fileout_local = new FileOutputStream(realPath.get() + "ip.txt");
										OutputStreamWriter outputWriter = new OutputStreamWriter(fileout_local);
										outputWriter.write(ip_local);
										outputWriter.close();
									} catch (Exception e) {
										e.printStackTrace();
									}
									try {
										fileout_local = new FileOutputStream(realPath.get() + "port.txt");
										OutputStreamWriter outputWriter = new OutputStreamWriter(fileout_local);
										outputWriter.write(port_local);
										outputWriter.close();
									} catch (Exception e) {
										e.printStackTrace();
									}
									Intent mainIntent_dev = new Intent(CacheUpdater.this, GameActivity.class);
									mainIntent_dev.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
									startActivity(mainIntent_dev);
									finish();
								}
							})
							.show();
                }
            });

            AlertDialog dialog = builder.create();

			dialog.show();
        }

        @Override
        protected void onProgressUpdate(String... values) {
            if (values.length == 1) {
                tv1.setText(values[0]);
            } else if (values.length == 2) {
                progressBar.setText(values[0] + " - " + Integer.parseInt(values[1]) + "%");
                progressBar.setProgress(Integer.parseInt(values[1]));
            }
        }

		private void downloadFile(File file, String prefix) {
			try {
				String fileURL = file.toString().replace(prefix, osConfig.CACHE_URL).replace(File.separator, "/");
				String description = getDescription(file);
				publishProgress("Downloading " + description, String.valueOf(0));
				HttpURLConnection connection = (HttpURLConnection) new URL(fileURL).openConnection();
				try (BufferedInputStream in = new BufferedInputStream(connection.getInputStream());
					 FileOutputStream fileOutputStream = new FileOutputStream(file)) {
					int filesize = connection.getContentLength();
					byte dataBuffer[] = new byte[1024];
					int bytesRead;
					int totalRead = 0;
					while ((bytesRead = in.read(dataBuffer, 0, 1024)) != -1) {
						totalRead += bytesRead;
						fileOutputStream.write(dataBuffer, 0, bytesRead);
						publishProgress("Downloading " + description, "" + (100*totalRead/filesize));
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
				connection.disconnect();
			} catch (Exception a) { a.printStackTrace(); }
		}

        public String getMD5Checksum(String filename) throws Exception {
            InputStream fis = openFileInput(filename);

            byte[] buffer = new byte[1024];
            MessageDigest complete = MessageDigest.getInstance("MD5");
            int numRead;

            do {
                numRead = fis.read(buffer);
                if (numRead > 0) {
                    complete.update(buffer, 0, numRead);
                }
            } while (numRead != -1);

            fis.close();

            byte[] b = complete.digest();
            StringBuilder result = new StringBuilder();

            for (byte aB : b) {
                result.append(Integer.toString((aB & 0xff) + 0x100, 16).substring(1));
            }
            return result.toString();
        }

        boolean verifyFile(String filename, String checksum) {
            return true;

        }
    }

    private final String[] nicename = {"Checksum", "3D models", "Application Icon", "Graphics", "Landscape", "library"};
    private final String[] normalName = {"MD5CHECKSUM", "models.orsc", "RuneScape.png", "Sprites.orsc", "Landscape.orsc", "library.orsc"};

    public String getNiceName(String s) {
        for (int i = 0; i < normalName.length; i++) {
            if (normalName[i].equalsIgnoreCase(s)) {
                return nicename[i];
            }
        }
        return "File";
    }

	private String getDescription(File ref) {
		int index = ref.getName().lastIndexOf('.');
		if (index == -1)
			return "General";
		else {
			String extension = ref.getName().substring(index + 1);
			if (extension.equalsIgnoreCase("ospr"))
				return "Graphics";
			else if (extension.equalsIgnoreCase("wav"))
				return "Audio";
			else if (extension.equalsIgnoreCase("orsc"))
				return "Graphics";
			else if (extension.equalsIgnoreCase("jar"))
				return "Executable";
			else
				return "General";
		}
	}
}
