package com.example.hyojin.myrecorder;

public class AppConfig {
    // Server user login url
    public static String URL_LOGIN = "http://115.71.232.67/android_login_api/login.php";

    // Server user register url
    public static String URL_REGISTER = "http://115.71.232.67/android_login_api/register.php";

    // Server emil authentication check url
    public static String URL_EMAIL= "http://115.71.232.67/android_login_api/sendmail.php";

    // Server emil authentication check url
    public static String URL_AUTHENTICATION= "http://115.71.232.67/android_login_api/athentication.php";

    // Check user name redundancy(중복확인) url
     public static String URL_CHECKONLY = "http://115.71.232.67/android_login_api/checkOnlyName.php";

    // Delete User url
    public static String URL_REMOVEUSER = "http://115.71.232.67/android_login_api/removeUser.php";

    // Create Streaming room url
    public static String URL_SETSTREAM= "http://128.199.177.183/myRecorder/StreamInfo.php";

    // Delete Streaming room url
    public static String URL_REMOVESTREAM= "http://128.199.177.183/myRecorder/StreamInfoDel.php";

    // Get Streaming Info url
    public static String URL_GETSTREAM= "http://128.199.177.183/myRecorder/GetStreamInfo.php";

}
