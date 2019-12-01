package com.anwesh.uiprojects.bouncyrotlineview

/**
 * Created by anweshmishra on 01/12/19.
 */

import android.view.View
import android.view.MotionEvent
import android.content.Context
import android.app.Activity
import android.graphics.Paint
import android.graphics.Canvas
import android.graphics.Color

val nodes : Int = 5
val lines : Int = 4
val scGap : Float = 0.02f
val strokeFactor : Int = 90
val foreColor : Int = Color.parseColor("#283593")
val backColor : Int = Color.parseColor("#BDBDBD")
val delay : Long = 30
val sizeFactor : Float = 2.9f

fun Int.inverse() : Float = 1f / this
fun Float.maxScale(i : Int, n : Int) : Float = Math.max(0f, this - i * n.inverse())
fun Float.divideScale(i : Int, n : Int) : Float = Math.min(n.inverse(), maxScale(i, n)) * n
fun Float.sinify() : Float = Math.sin(this * Math.PI).toFloat()

fun Canvas.drawRotLine(i : Int, scale : Float, size : Float, paint : Paint) {
    val sf : Float = scale.divideScale(1, 2).sinify()
    val sc : Float = scale.divideScale(0, 2)
    for (j in 0..1) {
        save()
        rotate(90f * i + 90f * sc + 45f * sf * (1f - 2 * j))
        drawLine(0f, 0f, size, 0f, paint)
        restore()
    }
}

fun Canvas.drawBRLNode(i : Int, scale : Float, paint : Paint) {
    val w : Float = width.toFloat()
    val h : Float = height.toFloat()
    val gap : Float = w / (nodes + 1)
    val size : Float = gap / sizeFactor
    paint.color = foreColor
    save()
    translate(gap * (i + 1), h / 2)
    val ij : Int = Math.floor((lines) * scale.toDouble()).toInt()
    drawRotLine(ij, scale.divideScale(ij, lines), size, paint)
    restore()
}

class BouncyRotLineView(ctx : Context) : View(ctx) {

    private val paint : Paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val renderer : Renderer = Renderer(this)

    override fun onDraw(canvas : Canvas) {
        renderer.render(canvas, paint)
    }

    override fun onTouchEvent(event : MotionEvent) : Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                renderer.handleTap()
            }
        }
        return true
    }

    data class State(var scale : Float = 0f, var dir : Float = 0f, var prevScale : Float = 0f) {

        fun update(cb : (Float) -> Unit) {
            scale += scGap * dir
            if (Math.abs(scale - prevScale) > 1) {
                scale = prevScale + dir
                dir = 0f
                prevScale = scale
                cb(prevScale)
            }
        }

        fun startUpdating(cb : () -> Unit) {
            if (dir == 0f) {
                dir = 1f - 2 * prevScale
                cb()
            }
        }
    }

    data class Animator(var view : View, var animated : Boolean = false) {

        fun animate(cb : () -> Unit) {
            if (animated) {
                cb()
                try {
                    Thread.sleep(delay)
                    view.invalidate()
                } catch (ex : Exception) {

                }
            }
        }

        fun start() {
            if (!animated) {
                animated = true
                view.postInvalidate()
            }
        }

        fun stop() {
            if (animated) {
                animated = false
            }
        }
    }

    data class BRLNode(var i : Int, val state : State = State()) {

        private var next : BRLNode? = null
        private var prev : BRLNode? = null

        init {
            addNeighbor()
        }

        fun addNeighbor() {
            if (i < nodes - 1) {
                next = BRLNode(i + 1)
                next?.prev = this
            }
        }

        fun draw(canvas : Canvas, paint : Paint) {
            canvas.drawBRLNode(i, state.scale, paint)
            next?.draw(canvas, paint)
        }

        fun update(cb : (Float) -> Unit) {
            state.update(cb)
        }

        fun startUpdating(cb : () -> Unit) {
            state.startUpdating(cb)
        }

        fun getNext(dir : Int, cb : () -> Unit) : BRLNode {
            var curr : BRLNode? = prev
            if (dir == 1) {
                curr = next
            }
            if (curr != null) {
                return curr
            }
            cb()
            return this
        }
    }

    data class BouncyRotLine(var i : Int) {

        private val root : BRLNode = BRLNode(0)
        private var curr : BRLNode = root
        private var dir : Int = 1

        fun draw(canvas : Canvas, paint : Paint) {
            root.draw(canvas, paint)
        }

        fun update(cb : (Float) -> Unit) {
            curr.update {
                curr = curr.getNext(dir) {
                    dir *= -1
                }
                cb(it)
            }
        }

        fun startUpdating(cb : () -> Unit) {
            curr.startUpdating(cb)
        }
    }

    data class Renderer(var view : BouncyRotLineView) {

        private val animator : Animator = Animator(view)
        private val brl : BouncyRotLine = BouncyRotLine(0)

        fun render(canvas : Canvas, paint : Paint) {
            canvas.drawColor(backColor)
            brl.draw(canvas, paint)
            animator.animate {
                brl.update {
                    animator.stop()
                }
            }
        }

        fun handleTap() {
            brl.startUpdating {
                animator.start()
            }
        }
    }
}
