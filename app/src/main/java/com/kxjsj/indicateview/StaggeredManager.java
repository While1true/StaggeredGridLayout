package com.kxjsj.indicateview;

import android.graphics.PointF;
import android.graphics.Rect;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.LinearSmoothScroller;
import android.support.v7.widget.OrientationHelper;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.util.SparseArray;
import android.view.View;
import android.view.ViewGroup;

import java.lang.reflect.Field;


/**
 * Created by vange on 2017/11/14.
 */

public class StaggeredManager extends RecyclerView.LayoutManager{

    private int count;
    int[] offsets;
    int scrolls;
    int maxHeight;
    private boolean plant;


    SparseArray<Integer> sizeArray = new SparseArray<>(5);

    Pool<Rect> layouts = new Pool<>(new Pool.Factory<Rect>() {
        @Override
        public Rect get() {
            return new Rect();
        }
    });
    private OrientationHelper helper;
    private OrientationHelper helper2;
    private int eachWidth;

    public StaggeredManager setCount(int count) {
        this.count = count;
        offsets = new int[count];
        return this;
    }
    public StaggeredManager setPlant(boolean plant) {
        this.plant = plant;
        return this;
    }
    RecyclerView.Adapter newAdapter;
    @Override
    public void onAdapterChanged(RecyclerView.Adapter oldAdapter, RecyclerView.Adapter newAdapter) {
        super.onAdapterChanged(oldAdapter, newAdapter);
        this.newAdapter=newAdapter;
        System.out.println("onAdapterChanged");
    }

    @Override
    public RecyclerView.LayoutParams generateDefaultLayoutParams() {
        return new RecyclerView.LayoutParams(eachWidth, ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    @Override
    public void onLayoutChildren(RecyclerView.Recycler recycler, RecyclerView.State state) {
        if (state.getItemCount() == 0) {
            detachAndScrapAttachedViews(recycler);
        }
        if (getChildCount() == 0 && state.isPreLayout()) {
            return;
        }

        if (helper == null) {
            helper = OrientationHelper.createHorizontalHelper(this);
            helper2 = OrientationHelper.createVerticalHelper(this);
        }

        detachAndScrapAttachedViews(recycler);


        /**
         * 预计算位置
         */
        Saveposition(recycler, state);

        layout(recycler, state, 0);

    }

    private void Saveposition(final RecyclerView.Recycler recycler, RecyclerView.State state) {
        offsets = new int[count];
        layouts.getArray().clear();
//        sizeArray.clear();
        eachWidth = helper.getTotalSpace() / count;

        caculate(recycler,state);

    }

    private void caculate(final RecyclerView.Recycler recycler,RecyclerView.State state) {
        long start=System.currentTimeMillis();
        A:
        for (int i = 0; i < getItemCount(); i++) {
            final int itemViewType = newAdapter.getItemViewType(i);
            /**
             * 之测量不同type的大小 计算位置
             */
            if (sizeArray.get(itemViewType) == null) {
                View scrap = recycler.getViewForPosition(i);
                addView(scrap);
                measureChildWithMargins(scrap, eachWidth, 0);
                int decoratedMeasuredHeight = getDecoratedMeasuredHeight(scrap);
                removeAndRecycleView(scrap, recycler);
                sizeArray.put(itemViewType, decoratedMeasuredHeight);
            }
            int rowNumber = getMinIndex();
            Rect rect = layouts.get(i);
            rect.set(rowNumber * eachWidth, offsets[rowNumber], (rowNumber + 1) * eachWidth, offsets[rowNumber] + sizeArray.get(itemViewType));
            offsets[rowNumber] = offsets[rowNumber] + rect.height();
            /**
             * 只多加载一屏幕的
             */
//            if (offsets[rowNumber] > getPaddingTop() + 2 * helper2.getTotalSpace()) {
//                break A;
//            }

            /**
             * 为了极端情况下的性能考虑，舍弃一些item,先专家在部分
             */
            if(System.currentTimeMillis()-start>400){
                try {
                    Class<? extends RecyclerView.State> aClass = state.getClass();
                    Field mItemCount= aClass.getDeclaredField("mItemCount");
                    mItemCount.setAccessible(true);
                    mItemCount.set(state,i);
                    System.out.println(i+"xxxxxxxxxxxxxxxxxxx");
                } catch (NoSuchFieldException e) {
                    e.printStackTrace();
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }

                break;
            }
        }
        maxHeight = getMaxHeight();
        if(plant) {
            setBottomSeemHeight();
        }
    }

    private void setBottomSeemHeight() {
        int minIndex = getMinIndex();
        for (int i = (getItemCount() - count - 1); i <getItemCount() ; i++) {
            Rect rect = layouts.get(i);
            int space = getPaddingTop() + helper2.getTotalSpace();
            if(rect.bottom> space) {
                rect.bottom = offsets[minIndex]>space?offsets[minIndex]:space;
                maxHeight=offsets[minIndex];
            }
        }
    }

    /**
     * 获取最小的指针位置
     *
     * @return
     */
    private int getMinIndex() {
        int min = 0;
        int minnum = offsets[0];
        for (int i = 1; i < offsets.length; i++) {
            if (minnum > offsets[i]) {
                minnum = offsets[i];
                min = i;
            }
        }
        return min;
    }

    /**
     * 获取最大的高度
     *
     * @return
     */
    private int getMaxHeight() {
        int max = offsets[0];
        for (int i = 1; i < offsets.length; i++) {
            if (offsets[i] > max) {
                max = offsets[i];
            }
        }
        return max;
    }

    public Rect getRect(RecyclerView.Recycler recycler, int position) {
        Rect rectx = layouts.get(position);
        return rectx;
    }


    /**
     * dy 1 上滑 -1 下滑 0出初始
     *
     * @param recycler
     * @param state
     */
    private void layout(RecyclerView.Recycler recycler, RecyclerView.State state, int dy) {
        Rect layoutRange = new Rect(getPaddingLeft(), getPaddingTop() + scrolls, helper.getTotalSpace() + getPaddingLeft(), helper2.getTotalSpace() + getPaddingTop() + scrolls);
        int itemCount = state.getItemCount();
        if (dy >= 0) {
            dolayoutAndRecycler(recycler, layoutRange, itemCount);
        } else {
            dolayoutAndRecyclerDown(recycler, layoutRange, itemCount);
        }


    }

    private void recyclerViews(RecyclerView.Recycler recycler) {
        detachAndScrapAttachedViews(recycler);
    }


    private void dolayoutAndRecyclerDown(RecyclerView.Recycler recycler, Rect layoutRange, int itemCount) {
        int childCount = getChildCount();
        int max = getMax(childCount);
        recyclerViews(recycler);
        boolean needbreak = false;
        int xx = 0;
        for (int i = max; i >= 0; i--) {
            Rect layout = getRect(recycler, i);
            if (layout.bottom > layoutRange.top && layout.top < layoutRange.bottom) {
                needbreak = true;
                View viewForPosition = recycler.getViewForPosition(i);
                addView(viewForPosition);
                measureChildWithMargins(viewForPosition, eachWidth, 0);
                layoutDecoratedWithMargins(viewForPosition, layout.left, layout.top - scrolls, layout.right, layout.bottom - scrolls);
            } else {
                if (needbreak) {
                    xx++;
                    if (xx > count) {
                        break;
                    }
                }
            }
        }
    }

    private int getMax(int childCount) {
        int max = 0;
        if (childCount != 0) {
            max = getPosition(getChildAt(0));
            for (int i = 1; i < childCount; i++) {
                int position = getPosition(getChildAt(i));
                if (position > max) {
                    max = position;
                }
            }
        }
        return max;
    }

    /**
     * 出初始layout
     *
     * @param recycler
     * @param layoutRange
     * @param itemCount
     */
    private void dolayoutAndRecycler(RecyclerView.Recycler recycler, Rect layoutRange, int itemCount) {
        int childCount = getChildCount();
        int min = getMin(childCount);
        System.out.println(childCount+"--------------------------------"+"xxx");
        recyclerViews(recycler);
        boolean needbreak = false;
        int xx = 0;
        A:for (int i = min; i < itemCount; i++) {
            final Rect layout = getRect(recycler, i);
//              if(doFlat(recycler,i,layoutRange)) {
//                  break A;
//              }
            if (layout.bottom > layoutRange.top && layout.top < layoutRange.bottom) {
                needbreak = true;
                View viewForPosition = recycler.getViewForPosition(i);
                addView(viewForPosition);
                measureChildWithMargins(viewForPosition, eachWidth, 0);
                layoutDecoratedWithMargins(viewForPosition, layout.left, layout.top - scrolls, layout.right, layout.bottom - scrolls);
            } else {
                if (needbreak) {
                    xx++;
                    if (xx > count) {
                        break A;
                    }
                }
            }

        }
    }

    @Deprecated
    private boolean doFlat(RecyclerView.Recycler recycler,int i, Rect layoutRange) {
        int itemCount = getItemCount();
        if(i==getItemCount()-count-1){
            B: for (int i1 = i; i1 < itemCount; i1++) {
                if(!Rect.intersects(layouts.get(i1),layoutRange)){

                   break B;
                }
                if(i1==itemCount-1){
                    int minIndex = getMinIndex();
                    int offset = offsets[minIndex];
                    for (int i2 = i; i2 < itemCount; i2++) {
                        int needshow=0;
                        Rect layout = layouts.get(i2);

                        if(layout.bottom>offset) {
                            needshow = offset - layout.top;
                            if (needshow < 0) {
                                needshow = 0;
                            }
                        }
                        View viewForPosition = recycler.getViewForPosition(i2);
                        addView(viewForPosition);
                        measureChildWithMargins(viewForPosition, eachWidth, needshow);

                        layoutDecoratedWithMargins(viewForPosition, layout.left, layout.top - scrolls, layout.right, offset-scrolls);
                    }
                    return true;
                }
            }
        }
        return false;
    }

    private int getMin(int childCount) {
        int min = 0;
        if (childCount != 0) {
            min = getPosition(getChildAt(0));
            for (int i = 1; i < childCount; i++) {
                int position = getPosition(getChildAt(i));
                if (position < min) {
                    min = position;
                }
            }
        }
        return min;
    }


    @Override
    public boolean canScrollVertically() {
        return true;
    }

    @Override
    public boolean canScrollHorizontally() {
        //返回true表示可以横向滑动
        return false;
    }

    @Override
    public int scrollVerticallyBy(int dy, RecyclerView.Recycler recycler, RecyclerView.State state) {

        if (maxHeight < helper2.getTotalSpace()) {
            return 0;
        }
        if (scrolls + dy > maxHeight - helper2.getTotalSpace()) {
         dy = maxHeight - helper2.getTotalSpace() - scrolls;
        }

        if (scrolls + dy < 0) {
            dy = -scrolls;
        }
        offsetChildrenVertical(-dy);
        scrolls += dy;
        if (dy > 0) {
            layout(recycler, state, 1);
        } else if (dy < 0) {
            layout(recycler, state, -1);
        }
        return dy;
    }

    @Override
    public void scrollToPosition(int position){
        int temp=position;
        if(position>getItemCount()){
            temp=getItemCount();
        }else if(position<0){
            temp=0;
        }
        int top = layouts.get(temp).top;
        if(top>maxHeight - helper2.getTotalSpace()){
            top=maxHeight - helper2.getTotalSpace();
        }
        scrolls=top;
        requestLayout();
    }
    @Override
    public void smoothScrollToPosition(RecyclerView recyclerView, RecyclerView.State state, int position) {//平滑的移动到某一项
        int top = layouts.get(position).top;
        int needscroll = top - scrolls;
        recyclerView.smoothScrollBy(0,needscroll);
    }
    private static class Pool<T> {
        SparseArray<T> array;
        Factory<T> tnew;

        public Pool(Factory<T> tnew) {
            array = new SparseArray<>();
            this.tnew = tnew;
        }

        public int size() {
            return array.size();
        }

        public SparseArray<T> getArray() {
            return array;
        }

        public T get(int key) {
            T t = array.get(key);
            if (t == null) {
                t = tnew.get();
                array.put(key, t);
            }
            return t;
        }

        public interface Factory<T> {
            T get();
        }
    }


}
