package com.feifan.planlib;

import android.os.Parcelable;

/**
 * 平面图坐标点
 * <pre>
 *     包含用于绘制的坐标点和用于计算位置的坐标点信息，绘制坐标点使用承载平面图的View坐标系
 *     位置坐标系使用平面图坐标系
 * </pre>
 *
 * Created by xuchunlei on 16/8/26.
 */
public interface ILayerPoint {
    /**
     * 设置索引编号
     * @param id
     */
    void setId(int id);
    /**
     * 获取索引编号
     * @return
     */
    int getId();

    /**
     * 获取平面图位置横坐标
     * @return
     */
    float getRawX();

    /**
     * 获取平面图位置纵坐标
     * @return
     */
    float getRawY();

    /**
     * 获取绘制位置横坐标
     * @return
     */
    float getLocX();

    /**
     * 获取绘制位置纵坐标
     * @return
     */
    float getLocY();

    /**
     * 获取真实位置横坐标
     * @return
     */
    float getRealX();

    /**
     * 获取真实位置纵坐标
     * @return
     */
    float getRealY();
    /**
     * 平移绘制坐标
     * @param deltaX
     * @param deltaY
     */
    void offset(float deltaX, float deltaY);

    /**
     * 平移平面图坐标
     * @param deltaX
     * @param deltaY
     */
    void offsetRaw(float deltaX, float deltaY);

    /**
     * 设置绘制坐标
     * @param x
     * @param y
     */
    void setDraw(float x, float y);

    /**
     * 设置平面图坐标
     * @param x
     * @param y
     */
    void setRaw(float x, float y);

    /**
     * 设置真实坐标
     * @param x
     * @param y
     */
    void setReal(float x, float y);

    /**
     * 设置比例尺
     * <p>
     *     平面图坐标与实际坐标的比值
     * </p>
     * @param scale
     */
    void setScale(float scale);

    /**
     * 是否可移动
     * @return
     */
    boolean isMovable();

    /**
     * 设置移动标记
     * @param movable
     */
    void setMovable(boolean movable);
}
