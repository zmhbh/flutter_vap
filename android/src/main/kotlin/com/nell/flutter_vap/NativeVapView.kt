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
import java.net.URL
import java.util.Random


internal class NativeVapView(binaryMessenger: BinaryMessenger, context: Context, id: Int, creationParams: Map<String?, Any?>?) : MethodChannel.MethodCallHandler, PlatformView {
    private val mContext: Context = context

    private val vapView: AnimView = AnimView(context)
    private var channel: MethodChannel
    private var methodResult: MethodChannel.Result? = null

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
                val srcTag = resource.tag
                if (srcTag.isNotEmpty()) {
                    val urlStr = "https://dbekpb58z4pug.cloudfront.net/img/5de243e0-8985-11ef-88a9-2971adb9b100.png?x-oss-process=image/resize,l_600"
                    val url = URL(urlStr)
                    val image = BitmapFactory.decodeStream(url.openStream())
                    result(image)

//                    val drawableId = if (head1Img) R.drawable.head1 else R.drawable.head2
//                    head1Img = !head1Img
//                    val options = BitmapFactory.Options()
//                    options.inScaled = false
//                    result(BitmapFactory.decodeResource(resources, drawableId, options))
                } else {
                    result(null)
                }
            }

            /**
             * 获取文字资源
             */
            override fun fetchText(resource: Resource, result: (String?) -> Unit) {
                val str = "恭喜 No.${1000 + Random().nextInt(8999)}用户 升神"
                val srcTag = resource.tag
                if (srcTag.isNotEmpty()) { // 此tag是已经写入到动画配置中的tag
                    result(str)
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
                call.argument<String>("path")?.let {
                    vapView.startPlay(File(it))
                }
            }
            "playAsset" -> {
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