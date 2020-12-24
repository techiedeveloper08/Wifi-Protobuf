package com.wifitask.utils

import android.annotation.SuppressLint
import android.content.Context
import android.os.AsyncTask
import java.io.*
import java.net.MalformedURLException
import java.net.URL


class FileManager {
    lateinit var context: Context

    //TODO read file from assets and convert into data class
    fun getFileFromAssets(fileName: String): BroStatusOuterClass.BroStatus {
        val messageString = context.assets.open(fileName).bufferedReader().use { it.readText() }
        return BroStatusOuterClass.BroStatus.parseFrom(messageString.toByteArray(Charsets.UTF_8))
    }

    //TODO read file from server and convert into data class
    @SuppressLint("StaticFieldLeak")
    fun getFileFromServer(onCallBack: (broStatus: BroStatusOuterClass.BroStatus) -> Unit) {
        val task = object: AsyncTask<Void, String, String?>() {
            override fun doInBackground(vararg params: Void?): String? {
                var returnString: String? = ""
                try {
                    val url = URL(AppConstant.BASEURL)
                    val bufferedReader = BufferedReader(InputStreamReader(url.openStream()))
                    while (bufferedReader.readLine() != null) {
                        returnString += bufferedReader.readLine()
                    }
                    bufferedReader.close()
                } catch (e: MalformedURLException) {
                    e.printStackTrace()
                } catch (e: IOException) {
                    e.printStackTrace()
                }
                return returnString
            }

            override fun onPostExecute(result: String?) {
                super.onPostExecute(result)
                try {
                    if (result != null) {
                        val data = BroStatusOuterClass.BroStatus.parseFrom(result.trim().toByteArray(Charsets.UTF_8))
                        onCallBack(data)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
        task.execute()
    }
}