package com.flyn.net.volcano;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.http.HttpEntity;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

public abstract class FileResponseHandler extends ResponseHandler
{
    private static final String TAG           = FileResponseHandler.class.getName();

    private static List<File>   mFileList     = Collections.synchronizedList(new ArrayList<File>(5));

    private long                bytesTotal    = 0;
    private long                bytesWritten  = 0;

    private File                mFile;
    private File                tempFile;

    private Timer               timer;
    private boolean             isScheduleing = true;
    private long                timeStamp     = System.currentTimeMillis();
    private long                sizeStamp     = 0;
    private int                 currentSpeed  = 0;
    private boolean             isContinue;

    public FileResponseHandler(Context context, String savePath, String fileName, boolean isContinue)
    {
        super();
        if (TextUtils.isEmpty(savePath) || TextUtils.isEmpty(fileName))
            return;

        this.isContinue = isContinue;

        this.mFile = new File(savePath, fileName);
        String tempname = fileName.substring(0, fileName.lastIndexOf("."));
        this.tempFile = new File(savePath, tempname + ".tmp");

        if (!this.mFile.getParentFile().exists())
            this.mFile.getParentFile().mkdirs();

    }

    public FileResponseHandler(Context context)
    {
        super();
        this.mFile = getTemporaryFile(context);
    }

    private File getTargetFile()
    {
        return this.mFile;
    }

    public boolean deleteTargetFile()
    {
        return getTargetFile() != null && getTargetFile().delete();
    }

    private File getTemporaryFile(Context context)
    {
        assert (context != null);
        try
        {
            return File.createTempFile("temp_", "_handled", context.getCacheDir());
        } catch (Throwable t)
        {
            Log.e(TAG, "Cannot create temporary file", t);
        }
        return null;
    }

    @Override
    protected void onSuccess(int statusCode, Map<String, String> headers, byte[] responseBody)
    {
        onSuccess(statusCode, headers, getTargetFile());
    }

    @Override
    protected void onFailure(int statusCode, Map<String, String> headers, byte[] responseBody, Throwable error)
    {
        onFailure(statusCode, headers, getTargetFile(), error);
    }

    public abstract void onSuccess(int statusCode, Map<String, String> headers, File file);

    public abstract void onFailure(int statusCode, Map<String, String> headers, File file, Throwable e);

    @Override
    protected byte[] entityToData(HttpEntity entity) throws IOException
    {
        if (null != entity)
        {
          

            long length = entity.getContentLength();
            this.bytesTotal = length != -1 ? length : entity.getContent().available();

            if (this.mFile.exists())
                throw new IOException("File already exists.");

            if (this.tempFile.exists())
            {
                if (this.isContinue)
                    this.bytesWritten = this.tempFile.length();
                else
                    this.tempFile.delete();
            }

            startTimer();
            BufferedInputStream inputStream = null;
            RandomAccessFile accessFile = null;

            try
            {
                if (mFileList.contains(this.mFile))
                    throw new IOException("This file is downloading.");
                else
                    mFileList.add(this.mFile);
                
                inputStream = new BufferedInputStream(entity.getContent());
                accessFile = new RandomAccessFile(this.tempFile, "rw");

                if (this.bytesWritten != 0)
                {
                    // 支持断点续传
                    accessFile.seek(this.bytesWritten);
//                    inputStream.skip(this.bytesWritten);
                } else
                {
                    // accessFile.setLength(this.bytesTotal);
                }

                int count;
                byte[] buffer = new byte[4096];

                while ((count = inputStream.read(buffer)) != -1)
                {
                    accessFile.write(buffer, 0, count);
                    updateProgress(count);
                }
                this.tempFile.renameTo(this.mFile);

            } catch (Exception e)
            {
                throw new IOException("entityToData exception:" + e.getMessage());
            } finally
            {
                stopTimer();
                if (null != accessFile)
                {
                    accessFile.close();
                    accessFile = null;
                }
                if (null != inputStream)
                {
                    inputStream.close();
                    inputStream = null;
                }
                if (null != this.mFile)
                    mFileList.remove(this.mFile);

            }

        }

        return null;
    }

    private void updateProgress(int count)
    {
        this.bytesWritten += count;
    }

    private void startTimer()
    {
        if (null == this.timer)
            this.timer = new Timer();

        final TimerTask task = new TimerTask()
        {
            @Override
            public void run()
            {
                if (isScheduleing)
                {
                    long nowTime = System.currentTimeMillis();
                    long spendTime = nowTime - timeStamp;
                    timeStamp = nowTime;

                    long getSize = bytesWritten - sizeStamp;
                    sizeStamp = bytesWritten;
                    if (spendTime > 0)
                        currentSpeed = (int) ((getSize / spendTime) / 1.024);

                    sendProgressMessage((int) bytesWritten, (int) bytesTotal, (int) currentSpeed);
                }
            }
        };
        this.timer.schedule(task, 500, 2000);
    }

    private void stopTimer()
    {
        this.isScheduleing = false;
        if (this.timer != null)
        {
            this.timer.cancel();
            this.timer = null;
        }
    }

}
