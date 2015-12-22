package uk.lancs.sharc.smat.service;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Environment;
import android.os.StatFs;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Date;

/**
 * Created by SHARC on 13/08/2015.
 */
public class ErrorReporter implements Thread.UncaughtExceptionHandler
{
    String filePath;
    private Thread.UncaughtExceptionHandler previousHandler;
    private static ErrorReporter mInstance;
    private Context curContext;

    public static ErrorReporter getInstance()
    {
        if (mInstance == null )
            mInstance = new ErrorReporter();
        return mInstance;
    }

    public void init(Context context)
    {
        previousHandler = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler(this);
        curContext = context;
        filePath = SharcLibrary.SHARC_LOG_FOLDER;
    }
    @Override
    public void uncaughtException(Thread t, Throwable e)
    {
        String report = "";
        Date curDate = new Date();
        report += "Error Report collected on: " + curDate.toString();
        report += "\n";
        report += "\n";
        report += "Device Information :";
        report += "\n";
        report += "==============";
        report += "\n";
        report += "\n";
        report += createInformationString();

        report += "\n\n";
        report += "Stack : \n";
        report += "======= \n";
        final Writer result = new StringWriter();
        final PrintWriter printWriter = new PrintWriter(result);
        e.printStackTrace(printWriter);
        String stacktrace = result.toString();
        report += stacktrace;

        // If the exception was thrown in a background thread inside
        // AsyncTask, then the actual exception can be found with getCause
        Throwable cause = e.getCause();
        if(cause != null)
        {
            report += "\n";
            report += "Cause : \n";
            report += "======= \n";
        }
        while (cause != null)
        {
            cause.printStackTrace( printWriter );
            report += result.toString();
            cause = cause.getCause();
        }
        printWriter.close();
        report += "****  End of current Report ***";
        saveAsFile(report);
        //SendErrorMail( Report );
        previousHandler.uncaughtException(t, e);
    }

    public String createInformationString()
    {
        String returnVal = "";
        try
        {
            PackageManager pm = curContext.getPackageManager();
            PackageInfo pi;
            pi = pm.getPackageInfo(curContext.getPackageName(), 0);

            returnVal += "Version : " + pi.versionName + "\n";
            returnVal += "Package : " + pi.packageName + "\n";
            returnVal += "FilePath : " + filePath + "\n";
            returnVal += "Phone Model:" + android.os.Build.MODEL + "\n";
            returnVal += "Android Version : " + android.os.Build.VERSION.RELEASE + "\n";
            returnVal += "Board : " + android.os.Build.BOARD + "\n";
            returnVal += "Brand : " + android.os.Build.BRAND + "\n";
            returnVal += "Device : " + android.os.Build.DEVICE + "\n";
            returnVal += "Display : " + android.os.Build.DISPLAY + "\n";
            returnVal += "Finger Print : " + android.os.Build.FINGERPRINT + "\n";
            returnVal += "Host : " + android.os.Build.HOST + "\n";
            returnVal += "ID : " + android.os.Build.ID + "\n";
            returnVal += "Model : " + android.os.Build.MODEL + "\n";
            returnVal += "Product : " + android.os.Build.PRODUCT + "\n";
            returnVal += "Tags : " + android.os.Build.TAGS + "\n";
            returnVal += "Time : " + android.os.Build.TIME + "\n";
            returnVal += "Type : " + android.os.Build.TYPE + "\n";
            returnVal += "User : " + android.os.Build.USER + "\n";
            returnVal += "Total Internal memory : " + getTotalInternalMemorySize() + "\n";
            returnVal += "Available Internal memory : " + getAvailableInternalMemorySize() + "\n";
        }
        catch( Exception e )
        {
            e.printStackTrace();
        }
        return returnVal;
    }

    public long getAvailableInternalMemorySize() {
        File path = Environment.getDataDirectory();
        StatFs stat = new StatFs(path.getPath());
        long blockSize = stat.getBlockSize();
        long availableBlocks = stat.getAvailableBlocks();
        return availableBlocks * blockSize;
    }

    public long getTotalInternalMemorySize() {
        File path = Environment.getDataDirectory();
        StatFs stat = new StatFs(path.getPath());
        long blockSize = stat.getBlockSize();
        long totalBlocks = stat.getBlockCount();
        return totalBlocks * blockSize;
    }

    private void sendErrorMail(String errorContent )
    {
        Intent sendIntent = new Intent(Intent.ACTION_SEND);
        String subject = "Crash with SMAT. Ticket number: #" + SharcLibrary.getReadableTimeStamp();
        String body = errorContent;
        sendIntent.putExtra(Intent.EXTRA_EMAIL, new String[] {"thesharcproject@gmail.com"});
        sendIntent.putExtra(Intent.EXTRA_TEXT, body);
        sendIntent.putExtra(Intent.EXTRA_SUBJECT, subject);
        sendIntent.setType("message/rfc822");
        curContext.startActivity( Intent.createChooser(sendIntent, "Title:") );
    }

    private void saveAsFile(String errorContent)
    {
        try
        {
            String fileName = filePath + "/stack-" + SharcLibrary.getReadableTimeStamp() + ".smatstacktrace";
            FileOutputStream trace = new FileOutputStream(fileName);
            trace.write(errorContent.getBytes());
            trace.close();
        }
        catch( Exception e )
        {
            // ...
        }
    }

    private String[] getErrorFileList()
    {
        File dir = new File(filePath);
        FilenameFilter filter = new FilenameFilter() {
            public boolean accept(File dir, String name) {
                return name.endsWith(".smatstacktrace");
            }
        };
        return dir.list(filter);
    }

    public boolean isThereAnyErrorFile()
    {
        String[] exceptionList = getErrorFileList();
        if(exceptionList == null)
            return false;
        else
            return exceptionList.length > 0;
    }

    public void sendReportEmail()
    {
        try
        {
            {
                String wholeErrorText = "";
                String[] errorFileList = getErrorFileList();
                int curIndex = 0;
                final int maxSendMail = 5;
                for (String curString : errorFileList)
                {
                    if ( curIndex++ <= maxSendMail )
                    {
                        BufferedReader input =  new BufferedReader(new FileReader(filePath + "/" + curString));
                        String line;
                        while (( line = input.readLine()) != null)
                        {
                            wholeErrorText += line + "\n";
                        }
                        input.close();
                    }
                    // DELETE FILES !!!!
                    File curFile = new File( filePath + "/" + curString );
                    curFile.delete();
                }
                sendErrorMail(wholeErrorText);
            }
        }
        catch( Exception e )
        {
            e.printStackTrace();
        }
    }
}