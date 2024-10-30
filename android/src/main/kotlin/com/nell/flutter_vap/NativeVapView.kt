package com.nell.flutter_vap

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.view.View
import com.tencent.qgame.animplayer.AnimConfig
import com.tencent.qgame.animplayer.AnimView
import com.tencent.qgame.animplayer.inter.IAnimListener
import com.tencent.qgame.animplayer.inter.IFetchResource
import com.tencent.qgame.animplayer.mix.Resource
import com.tencent.qgame.animplayer.util.ScaleType
import io.flutter.plugin.common.BinaryMessenger
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.platform.PlatformView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.File


internal class NativeVapView(binaryMessenger: BinaryMessenger, context: Context, id: Int, creationParams: Map<String?, Any?>?) : MethodChannel.MethodCallHandler, PlatformView {
    private val mContext: Context = context

    private val vapView: AnimView = AnimView(context)
    private var channel: MethodChannel
    private var methodResult: MethodChannel.Result? = null
    private var imageProperties: Map<String, String> = HashMap<String, String>()
    private var textProperties: Map<String, String> = HashMap<String, String>()


    init {
        vapView.setScaleType(ScaleType.FIT_CENTER)
        vapView.setAnimListener(object : IAnimListener {
            override fun onFailed(errorType: Int, errorMsg: String?) {
                GlobalScope.launch(Dispatchers.Main) {
                    methodResult?.success(HashMap<String, String>().apply {
                        put("status", "failure")
                        put("errorMsg", errorMsg ?: "unknown error")
                    })

                }
            }

            override fun onVideoComplete() {
                GlobalScope.launch(Dispatchers.Main) {
                    methodResult?.success(HashMap<String, String>().apply {
                        put("status", "complete")
                    })
                }
            }

            override fun onVideoDestroy() {
             
            }

            override fun onVideoRender(frameIndex: Int, config: AnimConfig?) {
            }

            override fun onVideoStart() {
            }

        }
        )

        vapView.setFetchResource(object: IFetchResource{
            /**
             * 获取图片资源
             * 无论图片是否获取成功都必须回调 result 否则会无限等待资源
             */
            override fun fetchImage(resource: Resource, result: (Bitmap?) -> Unit) {
                /**
                 * srcTag是素材中的一个标记，在制作素材时定义
                 * 解析时由业务读取tag决定需要播放的内容是什么
                 * 比如：一个素材里需要显示多个头像，则需要定义多个不同的tag，表示不同位置，需要显示不同的头像，文字类似
                 */
                try{
                    val srcTag = resource.tag
                    if (srcTag.isNotEmpty()) {
                        val imageCachePath = imageProperties[srcTag]
                        if (imageCachePath == null) {
                            methodResult?.success(HashMap<String, String>().apply {
                                put("status", "failure")
                                put("errorMsg", "imageProperty $srcTag is missing")
                            })
                            return
                        }

                        val image = BitmapFactory.decodeFile(imageCachePath)
                      /* the way of loading network image
                        val url = URL(imageCachePath)
                        val image = BitmapFactory.decodeStream(url.openStream())
                        */
                        result(image)

                    } else {
                        result(null)
                    }
                }catch (e: Exception){
                    result(null)
                }

            }

            /**
             * 获取文字资源
             */
            override fun fetchText(resource: Resource, result: (String?) -> Unit) {
                val srcTag = resource.tag
                if (srcTag.isNotEmpty()) { // 此tag是已经写入到动画配置中的tag
                    val textStr = textProperties[srcTag]
                    if (textStr == null) {
                        methodResult?.success(HashMap<String, String>().apply {
                            put("status", "failure")
                            put("errorMsg", "textProperty $srcTag is missing")
                        })
                        return
                    }

                    result(textStr)
                } else {
                    result(null)
                }
            }

            /**
             * 播放完毕后的资源回收
             */
            override fun releaseResource(resources: List<Resource>) {
                resources.forEach {
                    it.bitmap?.recycle()
                }
                imageProperties = HashMap<String, String>()
                textProperties = HashMap<String, String>()
            }
        })
        channel = MethodChannel(binaryMessenger, "flutter_vap_controller")
        channel.setMethodCallHandler(this)
    }

    override fun getView(): View {
        return vapView
    }

    override fun dispose() {
        channel.setMethodCallHandler(null)

    }

    override fun onMethodCall(call: MethodCall, result: MethodChannel.Result) {
        methodResult = result
        when (call.method) {
            "playPath" -> {
                call.argument<Map<String, String>>("imageProperties")?.let { this.imageProperties = it }
                call.argument<Map<String, String>>("textProperties")?.let { this.textProperties = it }
                call.argument<String>("path")?.let {
                    vapView.startPlay(File(it))
                }
            }
            "playAsset" -> {
               call.argument<Map<String, String>>("imageProperties")?.let { this.imageProperties = it }
                call.argument<Map<String, String>>("textProperties")?.let { this.textProperties = it }
                call.argument<String>("asset")?.let {
                    vapView.startPlay(mContext.assets, "flutter_assets/$it")
                }
            }
            "stop" -> {
                vapView.stopPlay()
            }
        }
    }


}