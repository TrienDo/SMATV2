package uk.lancs.sharc.smat.service;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringReader;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import uk.lancs.sharc.smat.R;
import uk.lancs.sharc.smat.model.AndroidWebViewInterface;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.database.Cursor;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;

import android.graphics.PorterDuff.Mode;
import android.graphics.Typeface;

import android.location.Location;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore.MediaColumns;
import android.util.Log;
import android.view.WindowManager;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

/**
 * <p>This class provides static methods for the whole application</p>
 * <p>These static methods are utilities </p>
 *
 * Author: Trien Do
 * Date: Feb 2014
 **/
public class SharcLibrary
{
	//Get all URL from a string
	public static final String SHARC_FOLDER = Environment.getExternalStorageDirectory().getAbsolutePath() + "/Sharc";
    public static final String SHARC_MEDIA_FOLDER = Environment.getExternalStorageDirectory().getAbsolutePath() + "/Sharc/Media";
	public static final String SHARC_LOG_FOLDER = Environment.getExternalStorageDirectory().getAbsolutePath() + "/Sharc/Logs";
	public static final String SHARC_DATABASE_FOLDER = Environment.getExternalStorageDirectory().getAbsolutePath() + "/Sharc/Databases";
	public static final String SHARC_POI_NORMAL = "normal";
	public static final String SHARC_POI_ACCESSIBILITY = "accessibility";
	public static String APP_PREFERENCES = "SmatPreferences";
	public static String PREFERENCES_ACCOUNTID = "accountId";
	public static String PREFERENCES_ACCOUNTTYPE = "accountType";

	@SuppressLint("SetJavaScriptEnabled")//allow interacting with Android code
	public static void setupWebView(WebView mWebview, Activity context, AndroidWebViewInterface interfaceObj)
	{
		mWebview.setWebChromeClient(new WebChromeClient());//support HTML 5
		mWebview.setWebViewClient(new WebViewClient());//Open link within webview
		mWebview.getSettings().setDefaultFontSize(20);
		//mWebview.getSettings().setBuiltInZoomControls(true);
		mWebview.getSettings().setPluginState(WebSettings.PluginState.ON);
		mWebview.getSettings().setJavaScriptEnabled(true);
		mWebview.addJavascriptInterface(interfaceObj,"Android");
		if(context!=null)
			context.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
				+ WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD|
				+ WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED|
				+ WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);

	}

	@SuppressLint("SetJavaScriptEnabled")
	public static void setupWebView(WebView mWebview, Activity context)
	{
		mWebview.setWebChromeClient(new WebChromeClient());//support HTML 5
		mWebview.setWebViewClient(new WebViewClient());//Open link within webview
		mWebview.getSettings().setDefaultFontSize(20);
		//mWebview.getSettings().setBuiltInZoomControls(true);
		mWebview.getSettings().setPluginState(WebSettings.PluginState.ON);
		mWebview.getSettings().setJavaScriptEnabled(true);
		if(context!=null)
			context.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
					+ WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD|
					+ WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED|
					+ WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);

	}

	public static String getIdString(String accountId){
		return String.valueOf((new Date()).getTime()) + "_" + accountId;
	}

	//id + itemType are used to identify which media or response is commented
	//itemType = media vs. response
	public static String getHTMLCodeForMedia(String id,String itemType,int noLike, int noComment, String type, String content, String name, boolean isLocal)
	{
		String strMedia = "<span id='#" + id + "#' name='#"  + itemType + "#" + noLike + "#" + noComment + "#'></span>"; ////000#id#1111#type#noLike#noComments#therest
		String path = "";
		if(isLocal)
			path = content;     //string for text media else path to the local media (photo, audio, video)
		//if(!isLocal && !type.equalsIgnoreCase("text"))
		if(!isLocal)
			path = SharcLibrary.SHARC_MEDIA_FOLDER + content.substring(content.lastIndexOf("/"));//media cached locally
		//Show text media in form of Title + Content, else Content + title
		if(type.equalsIgnoreCase("text"))
		{
			//strMedia += "<p style='margin-left:20px'>" +  content.replaceAll("(\r\n|\n)", "<br />") + "</p>";
			//strMedia += "<div><object style='width: 100%; height: 300px; overflow: auto' type='text/html' data='" + path + "' ></object></div>";
			try {
				strMedia += "<div style='margin-left:20px'>" +  SharcLibrary.readTextFile(new FileInputStream(path))+  "</div>";
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			}
		}
		else if(type.equalsIgnoreCase("image"))
		{
			strMedia += "<div align='center'><img hspace='20' width='90%' src='" +  path + "' /></div>";
		}
		else if(type.equalsIgnoreCase("audio"))
		{
			strMedia += "<div align='center'><audio style='margin-left:20px;' width='90%' controls><source src='" + path + "' type='audio/mpeg'></audio></div>";
		}
		else if(type.equalsIgnoreCase("video"))
		{
			strMedia += "<div align='center'><video style='margin-left:20px;' width='90%' poster controls><source src='" + path + "'></video></div>";
		}

		if(!type.equalsIgnoreCase("text"))
			strMedia += "<p style='margin-left:20px;font-weight: bold;'>" +  name + "</p>";
		return strMedia;
	}

    public static void copyFile(InputStream in, OutputStream out) throws Exception {
        try {
            byte[] buffer = new byte[1024];
            int read;
            while ((read = in.read(buffer)) != -1) {

                out.write(buffer, 0, read);

            }
        } catch (IOException e) {
            e.printStackTrace();
			throw new Exception();
        }
    }

	public static String getFilesize(String localPath, boolean isImage)
	{
		File localFile = new File(localPath);
		if(localFile.exists()) {
			double fileSize = localFile.length() / (1024.0 * 1024);
			if(isImage)
				fileSize *= 0.25;
			return String.format("%.2f", fileSize);
		}
		return "0";
	}

	public static String getStringSize(String content)
	{
		double fileSize = content.length() / 1024.0;
		return String.format("%.2f", fileSize);
	}


 	public static String getDropboxDirectLink(String url)
	{
		String publicURL = url.replace("https://www.drop", "https://dl.drop");
		publicURL = publicURL.substring(0, publicURL.indexOf("?"));
		return publicURL;
	}
	public static String getReadableTimeStamp()	{
		DateFormat dateFormat = new SimpleDateFormat("ddMMyyyy_HHmmss");
		//get current date time with Date()
		Date date = new Date();
		return dateFormat.format(date).toString();
	}

	public static String getMySQLDateStamp()	{
		DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
		//get current date time with Date()
		Date date = new Date();
		return dateFormat.format(date).toString();
	}

	public static int hex2Argb(int alpha, String colorStr)
    {
        return Color.argb(
        		alpha,
        		Integer.valueOf(colorStr.substring(1,3),16),
        		Integer.valueOf(colorStr.substring(3,5),16),
        		Integer.valueOf(colorStr.substring(5,7),16)
        );
    }
    
    public static int hex2rgb(String colorStr)
    {
        return Color.rgb(
        		Integer.valueOf(colorStr.substring(1,3),16),
        		Integer.valueOf(colorStr.substring(3,5),16),
        		Integer.valueOf(colorStr.substring(5,7),16)
        );
    }
    
    public static boolean isCurrentPointInsideRegion(LatLng curPoint, List<LatLng> pointArray)	
    {       
	       int i;
	       double angle=0;
	       double point1_lat;
	       double point1_long;
	       double point2_lat;
	       double point2_long;	        
	       int n = pointArray.size();
	       
	       for (i = 0; i < n; i++) {
	          point1_lat = pointArray.get(i).latitude - curPoint.latitude;
	          point1_long = pointArray.get(i).longitude - curPoint.longitude;
	          point2_lat = pointArray.get((i+1)%n).latitude - curPoint.latitude;	 
	          point2_long = pointArray.get((i+1)%n).longitude - curPoint.longitude;
	          
	          angle += Angle2D(point1_lat,point1_long,point2_lat,point2_long);
	       }

	       if (Math.abs(angle) < Math.PI)
	          return false;
	       else
	          return true;
	}
    
    public static double Angle2D(double y1, double x1, double y2, double x2)
	{
	   double dtheta, theta1, theta2;
	   theta1 = Math.atan2(y1,x1);
	   theta2 = Math.atan2(y2,x2);
	   dtheta = theta2 - theta1;
	   while (dtheta > Math.PI)
	      dtheta -= Math.PI * 2;
	   while (dtheta < -Math.PI)
	      dtheta += Math.PI * 2;

	   return(dtheta);
	}
    
  	//get file name from URI
    public static String getFileNameByUri(Context context, Uri uri)
	{
    	String filePath;
        String[] filePathColumn = {MediaColumns.DATA};
        Cursor cursor = context.getContentResolver().query(uri, filePathColumn, null, null, null);
        if(cursor!=null)
        {
	        cursor.moveToFirst();
	        int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
	        filePath = cursor.getString(columnIndex);
	        cursor.close();
	        return filePath;
        }
        else
        	return null;
	}
	
    public static ArrayList<String> extractLinksFromText(String text) 
	{
        ArrayList<String> uniqueLinks = new ArrayList<String>();
        HashSet<String> links = new HashSet<String>();
        
        String regex = "\\(?\\b(http://|https://|www[.])[-A-Za-z0-9+&@#/%?=~_()|!:,.;]*[-A-Za-z0-9+&@#/%=~_()|]";
        Pattern p = Pattern.compile(regex);
        Matcher m = p.matcher(text);
        while(m.find())   
        {
        	String urlStr = m.group();
        	if (urlStr.startsWith("(") && urlStr.endsWith(")"))
        	{
        		char[] stringArray = urlStr.toCharArray(); 
        		char[] newArray = new char[stringArray.length-2];
        		System.arraycopy(stringArray, 1, newArray, 0, stringArray.length-2);
        		urlStr = new String(newArray);
        		//System.out.println("Finally Url ="+newArray.toString());
        	}
        	//System.out.println("...Url..."+urlStr);
        	String tmpStr = urlStr.toLowerCase();
        	if (
        			tmpStr.endsWith(".jpg") || tmpStr.endsWith(".png") || tmpStr.endsWith(".jpeg") || 
        			tmpStr.endsWith(".bmp") || tmpStr.endsWith(".ico") || tmpStr.endsWith(".gif")  || 
        			tmpStr.endsWith(".mp3") || tmpStr.endsWith(".mp4") || tmpStr.endsWith(".ogg")  ||
        			tmpStr.endsWith(".wav") || tmpStr.endsWith(".swf") || tmpStr.endsWith(".webm") 
        		) 
        	{
        		//System.out.println("...Url..."+urlStr);
            	links.add(urlStr);
        	}
        	
        }
        //Remove duplicate        
        uniqueLinks.addAll(links);
        links.clear();
        links = null;
        return uniqueLinks;
    }

	public static Bitmap getThumbnail(String imageID)
	{		
		if(imageID == null)
			return null;
		try
		{
			//Access the corresponding image on local storage
            //String photoPath = SHARC_MEDIA_FOLDER + "/" +  imageID;
            //Drawable icon = Drawable.createFromPath(sdcard);            
        	Bitmap b = SharcLibrary.getBitmapFromFile(imageID);
        	if(b!=null && Math.min(b.getWidth(),b.getHeight()) >= 80)//need big enough image - not logo
        	{
	        	int scaleW = (int)Math.floor(b.getWidth()/90);
	        	int scaleH = (int)Math.floor(b.getHeight()/90);
	        	scaleW = Math.max(scaleW, scaleH);        		
	        	Bitmap sBitmap = Bitmap.createScaledBitmap(b, b.getWidth()/scaleW,b.getHeight()/scaleW, false);        	
	            return getRoundedCornerBitmap(sBitmap, Color.WHITE, 7, 3);
        	}		        	
		}
		catch(Exception e)
		{
			Log.e("getFirstImage:", e.getMessage());
			return null;
		}	
		return null;
	}
	
	public static boolean isNetworkAvailable(Context ctx) {
        ConnectivityManager connectivityManager 
              = (ConnectivityManager) ctx.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }
	
	public static boolean createFolder(String path)
	{
        File dir = new File(path);
        dir.mkdir();
        return true;
	}
	
	public static Bitmap drawTextToBitmap(String gText) 
	{    
	    Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
	    paint.setColor(Color.BLUE);
	    paint.setTextSize(25);
	    paint.setTypeface(Typeface.DEFAULT_BOLD);
	    paint.setShadowLayer(1f, 0f, 1f, Color.WHITE);

	    Rect bounds = new Rect();
	    paint.getTextBounds(gText, 0, gText.length(), bounds);
	    
	    Bitmap bitmap = Bitmap.createBitmap(bounds.width(), bounds.height()*2,Bitmap.Config.ARGB_8888);
	    Canvas canvas = new Canvas(bitmap);
	    
	    int x = (bitmap.getWidth() - bounds.width())/2;
	    int y = (bitmap.getHeight() + bounds.height())/2;

	    canvas.drawText(gText, x, y, paint);

	    return bitmap;
	}
	
	public static Bitmap getBitmapFromFile(String photoPath) {
		BitmapFactory.Options options = new BitmapFactory.Options();
		options.inPreferredConfig = Bitmap.Config.ARGB_8888;
		Bitmap bitmap = BitmapFactory.decodeFile(photoPath, options);		
		return bitmap;
	}	
	
	
	public static Bitmap scaleDownImage(String photoPath, int size)
	{	
		try
		{			            
        	Bitmap b = SharcLibrary.getBitmapFromFile(photoPath);        	
        	int scaleW = (int)Math.floor(b.getWidth()/size);
        	int scaleH = (int)Math.floor(b.getHeight()/size);
        	scaleW = Math.max(scaleW, scaleH);        		
        	Bitmap sBitmap = Bitmap.createScaledBitmap(b, b.getWidth()/scaleW,b.getHeight()/scaleW, false);        	
            return sBitmap;        			     
		}
		catch(Exception e)
		{
			Log.e("ScaleDownImage:", e.getMessage());
			return null;
		}						
	} 
	public static Bitmap getRoundedCornerBitmap(Bitmap bitmap, int color, int cornerSizePx, int borderSizePx) 
	{
	    try
	    {
			Bitmap output = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), Bitmap.Config.ARGB_8888);
		    Canvas canvas = new Canvas(output);
		    
		    Paint paint = new Paint();
		    Rect rect = new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight());
		    RectF rectF = new RectF(rect);
	
		    // prepare canvas for transfer
		    paint.setAntiAlias(true);
		    paint.setColor(0xFFFFFFFF);
		    paint.setStyle(Paint.Style.FILL);
		    canvas.drawARGB(0, 0, 0, 0);
		    canvas.drawRoundRect(rectF, cornerSizePx, cornerSizePx, paint);
	
		    // draw bitmap
		    paint.setXfermode(new PorterDuffXfermode(Mode.SRC_IN));
		    canvas.drawBitmap(bitmap, rect, rect, paint);
	
		    // draw border
		    paint.setColor(color);
		    paint.setStyle(Paint.Style.STROKE);
		    paint.setStrokeWidth((float) borderSizePx);
		    canvas.drawRoundRect(rectF, cornerSizePx, cornerSizePx, paint);
		    return output;
	    }
	    catch(Exception e)
	    {
	    	 e.printStackTrace();
	    }

	    return null;
	}
	public static String readTextFile(InputStream inStream)
	{		
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		try 
	    {
			InputStream inputStream = inStream;			
		    byte buf[] = new byte[1024];
		    int len;
	    
	        while ((len = inputStream.read(buf)) != -1) 
	        {
	            outputStream.write(buf, 0, len);
	        }
	        outputStream.close();
	        inputStream.close();
	    } 
	    catch (IOException e) 
	    {
	    	Log.d("Reading file:", e.getMessage());
	    }
	    return outputStream.toString();
	}
	public static Document getDomElement(String xml)
	{
        Document doc = null;
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        try 
        { 
            DocumentBuilder db = dbf.newDocumentBuilder(); 
            InputSource is = new InputSource();
            is.setCharacterStream(new StringReader(xml));
            doc = db.parse(is); 
        } 
        catch (Exception e) 
        {
        	Log.d("Get DOM:", e.getMessage());
        	return null;
        }        
        return doc;
    }
	
	public static ArrayList<LatLng> getPathFromXMLDom(Document xmlDOM, String tagName)
	{
		ArrayList<LatLng> path = new ArrayList<LatLng>();
		NodeList allPoints = xmlDOM.getElementsByTagName(tagName);
		if(allPoints == null)
			return null;
		for(int i = 0; i < allPoints.getLength(); i++)
		{
			String[] coorString = allPoints.item(i).getFirstChild().getNodeValue().split(" ");
			path.add(new LatLng(Double.parseDouble(coorString[1]), Double.parseDouble(coorString[0])));
		}
		return path;
	}
	
	public static void drawRoute(GoogleMap mMap, ArrayList<LatLng> path, int pathWidth, int pathColor)
	{
		ArrayList<LatLng> simplifiedPath = smoothPath(8,path);
		for(int i=0; i < 6; i++)
			simplifiedPath.remove(simplifiedPath.size()-1);
		
		simplifiedPath.add(simplifiedPath.get(0));
		mMap.addPolyline((new PolylineOptions()		
		    .width(pathWidth)
		    .color(pathColor))
		    ).setPoints(simplifiedPath);
		
		//add arrow
		addSharcArrow(mMap,simplifiedPath.get(0), 0.0f,BitmapDescriptorFactory.fromResource(R.raw.start));	
		addSharcArrow(mMap,simplifiedPath.get(1), 180.0f,BitmapDescriptorFactory.fromResource(R.raw.arrow));
		addSharcArrow(mMap,simplifiedPath.get(6), 195.0f,BitmapDescriptorFactory.fromResource(R.raw.arrow));
		addSharcArrow(mMap,simplifiedPath.get(12), 100.0f,BitmapDescriptorFactory.fromResource(R.raw.arrow));
		addSharcArrow(mMap,simplifiedPath.get(18), 260.0f,BitmapDescriptorFactory.fromResource(R.raw.arrow));
		addSharcArrow(mMap,simplifiedPath.get(21), 330.0f,BitmapDescriptorFactory.fromResource(R.raw.arrow));
		//addSharcArrow(mMap,simplifiedPath.get(simplifiedPath.size()-1), 0.0f,BitmapDescriptorFactory.fromResource(R.raw.end));
			
	}
	/**
	* Decimates the given locations for a given zoom level. This uses a
	* Douglas-Peucker decimation algorithm.
	* 
	* @param tolerance in meters
	* @param locations input
	*/
	
	public static ArrayList<LatLng> smoothPath(double tolerance, ArrayList<LatLng> locations) 
	{
		final int n = locations.size();
	    if (n < 1) {
	      return locations;
	    }
	    ArrayList<LatLng> decimated = new ArrayList<LatLng>();
	    int idx;
	    int maxIdx = 0;
	    Stack<int[]> stack = new Stack<int[]>();
	    double[] dists = new double[n];
	    dists[0] = 1;
	    dists[n - 1] = 1;
	    double maxDist;
	    double dist = 0.0;
	    int[] current;

	    if (n > 2) {
	      int[] stackVal = new int[] { 0, (n - 1) };
	      stack.push(stackVal);
	      while (stack.size() > 0) {
	        current = stack.pop();
	        maxDist = 0;
	        for (idx = current[0] + 1; idx < current[1]; ++idx) 
	        {
	        	dist = distance(locations.get(idx), locations.get(current[0]), locations.get(current[1]));
	        	if (dist > maxDist) {
	        		maxDist = dist;
	        		maxIdx = idx;
	        	}
	        }
	        if (maxDist > tolerance) {
	          dists[maxIdx] = maxDist;
	          int[] stackValCurMax = { current[0], maxIdx };
	          stack.push(stackValCurMax);
	          int[] stackValMaxCur = { maxIdx, current[1] };
	          stack.push(stackValMaxCur);
	        }
	      }
	    }

	    int i = 0;
	    idx = 0;
	    decimated.clear();
	    for (LatLng l : locations) {
	      if (dists[idx] != 0) {
	        decimated.add(l);
	        i++;
	      }
	      idx++;
	    }
	    Log.d("Douglas-Peucker", "Decimating " + n + " points to " + i + " w/ tolerance = " + tolerance);
	    return decimated;
	  }
	
	public static double distance(final LatLng cc0, final LatLng cc1, final LatLng cc2) {
		Location c0 = new Location(""); c0.setLatitude(cc0.latitude);c0.setLongitude(cc0.longitude);
		Location c1 = new Location(""); c1.setLatitude(cc1.latitude);c1.setLongitude(cc1.longitude);
		Location c2 = new Location(""); c2.setLatitude(cc2.latitude);c2.setLongitude(cc2.longitude);
		
	    if (c1.equals(c2)) {
	      return c2.distanceTo(c0);
	    }

	    final double s0lat = c0.getLatitude() * Math.PI / 180.0;
	    final double s0lng = c0.getLongitude() * Math.PI / 180.0;
	    final double s1lat = c1.getLatitude() * Math.PI / 180.0;
	    final double s1lng = c1.getLongitude() * Math.PI / 180.0;
	    final double s2lat = c2.getLatitude() * Math.PI / 180.0;
	    final double s2lng = c2.getLongitude() * Math.PI / 180.0;

	    double s2s1lat = s2lat - s1lat;
	    double s2s1lng = s2lng - s1lng;
	    final double u = ((s0lat - s1lat) * s2s1lat + (s0lng - s1lng) * s2s1lng)
	        / (s2s1lat * s2s1lat + s2s1lng * s2s1lng);
	    if (u <= 0) {
	      return c0.distanceTo(c1);
	    }
	    if (u >= 1) {
	      return c0.distanceTo(c2);
	    }
	    Location sa = new Location("");
	    sa.setLatitude(c0.getLatitude() - c1.getLatitude());
	    sa.setLongitude(c0.getLongitude() - c1.getLongitude());
	    Location sb = new Location("");
	    sb.setLatitude(u * (c2.getLatitude() - c1.getLatitude()));
	    sb.setLongitude(u * (c2.getLongitude() - c1.getLongitude()));
	    return sa.distanceTo(sb);
	  }
	
	public static void addSharcArrow(GoogleMap mMap, LatLng pos, float rotDegree, BitmapDescriptor icon) 
	{
		mMap.addMarker(new MarkerOptions()
	        .position(pos)	        
	        .icon(icon)
	        .anchor(0.5f,0.5f)
	        .rotation(rotDegree)
	        .icon(icon)
	    );
	}
	public static LatLng locationToLatLng(Location loc) {
		if(loc != null)
			return new LatLng(loc.getLatitude(), loc.getLongitude());
		return null;
	}
}
