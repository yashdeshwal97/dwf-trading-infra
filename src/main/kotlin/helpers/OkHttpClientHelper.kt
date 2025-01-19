package helpers

import okhttp3.ConnectionPool
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import java.net.Proxy
import java.util.concurrent.TimeUnit

object OkHttpClientHelper {

    val okHttpClient = OkHttpClient()

    fun getClient() : OkHttpClient {

//        val interceptor = HttpLoggingInterceptor()
//        interceptor.level = HttpLoggingInterceptor.Level.BODY
//
        return okHttpClient.newBuilder()
                .connectionPool(ConnectionPool(50, 5, TimeUnit.MINUTES))
//                .addInterceptor(interceptor)
                .build()
    }

    fun getClient(pingInterval: Long, proxy: Proxy): OkHttpClient {
        return okHttpClient.newBuilder().pingInterval(pingInterval, TimeUnit.SECONDS)
            .proxy(proxy).retryOnConnectionFailure(true).build()
    }
}