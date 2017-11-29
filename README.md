

---
##### 瀑布流系统是本身自带的，但在实际使用中会有诸多问题
> 比如：在底部刷新了之后，回到顶部，就参差不齐了，空白 item交换等。

> 造轮子是为了解决这个问题，提供实际问题的解决思路。相比系统自带完善的layoutManger 还差太远太远

### 效果
[测试项目github地址](https://github.com/While1true/StaggeredGridLayout)

![2017-11-29-10-16-12.gif](http://upload-images.jianshu.io/upload_images/6456519-14502c98915f5d82.gif?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)


### 实现
- 自定义一个LayoutManager主要重写以下方法
1. generateDefaultLayoutParams()
2. onLayoutChildren
3. scrollVerticallyBy  canScrollVertically() 当然水平方向也有
- 核心是onLayoutChildren scrollVerticallyBy 

### 

```
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
        init(recycler, state);

        layout(recycler, state, 0);

    }

    private void init(final RecyclerView.Recycler recycler, RecyclerView.State state) {
        offsets = new int[count];
        attchedViews.clear();
        eachWidth = helper.getTotalSpace() / count;
    }

    private void caculate(final RecyclerView.Recycler recycler, int dy) {
        long start = System.currentTimeMillis();
        A:
        for (int i = layouts.size(); i < getItemCount(); i++) {
            /**
             * 之测量不同type的大小 计算位置
             */
            View scrap = recycler.getViewForPosition(i);
            addView(scrap);
            measureChildWithMargins(scrap, eachWidth, 0);
            int decoratedMeasuredHeight = getDecoratedMeasuredHeight(scrap);
            removeAndRecycleView(scrap, recycler);
            int rowNumber = getMinIndex();
            Rect rect = layouts.get(i);
            rect.set(rowNumber * eachWidth, offsets[rowNumber], (rowNumber + 1) * eachWidth, offsets[rowNumber] + decoratedMeasuredHeight);
            offsets[rowNumber] = offsets[rowNumber] + rect.height();
            /**
             * 只多加载一屏幕的
             */
            if (offsets[getMinIndex()] > dy + scrolls + getPaddingTop() + helper2.getTotalSpace()) {
                break A;
            }

        }
        maxHeight = getMaxHeight();
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

    private void recyclerViews(RecyclerView.Recycler recycler,Rect layoutrect) {
//        detachAndScrapAttachedViews(recycler);
        int childCount = getChildCount();
        for (int i = 0; i < childCount; i++) {
            View childAt = getChildAt(i);
            int position = getPosition(childAt);
            Rect rect = layouts.get(position);
            if(!Rect.intersects(rect,layoutrect)){
                attchedViews.remove(position);
                removeAndRecycleView(childAt,recycler);
                childCount--;
            }
        }
    }


    private void dolayoutAndRecyclerDown(RecyclerView.Recycler recycler, Rect layoutRange, int itemCount) {
        int childCount = getChildCount();
        int max = getMax(childCount);
        recyclerViews(recycler,layoutRange);
        int xx = 0;
        for (int i = max; i >= 0; i--) {
            Rect layout = getRect(recycler, i);
            if (Rect.intersects(layout, layoutRange)&&attchedViews.get(i)==null) {
                View viewForPosition = recycler.getViewForPosition(i);
                addView(viewForPosition);
                attchedViews.put(i,viewForPosition);
                measureChildWithMargins(viewForPosition, eachWidth, 0);
                layoutDecoratedWithMargins(viewForPosition, layout.left, layout.top - scrolls, layout.right, layout.bottom - scrolls);
            }
            if (layout.bottom <= layoutRange.top) {
                xx++;
                if (xx >= count) {
                    break;
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
        recyclerViews(recycler,layoutRange);
        int xx = 0;
        A:
        for (int i = min; i < itemCount; i++) {

            if (getRect(recycler, i).isEmpty()) {
                layouts.getArray().remove(i);
                caculate(recycler, 0);
            }

            final Rect layout = getRect(recycler, i);
            if (Rect.intersects(layout, layoutRange)&&attchedViews.get(i)==null) {
                View viewForPosition = recycler.getViewForPosition(i);
                addView(viewForPosition);
                attchedViews.put(i,viewForPosition);
                measureChildWithMargins(viewForPosition, eachWidth, 0);
                layoutDecoratedWithMargins(viewForPosition, layout.left, layout.top - scrolls, layout.right, layout.bottom - scrolls);
            }
            if (layout.top >= layoutRange.bottom) {
                xx++;
                if (xx >= count) {
                    break;
                }
            }
        }
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
        System.out.println(getChildCount() + "------------");
        if (layouts.size() < getItemCount() && maxHeight <= dy + scrolls + getPaddingTop() + helper2.getTotalSpace()) {
            caculate(recycler, dy);
        }

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
    public void scrollToPosition(int position) {
        int temp = position;
        if (position > getItemCount()) {
            temp = getItemCount();
        } else if (position < 0) {
            temp = 0;
        }
        int top = layouts.get(temp).top;
        if (top > maxHeight - helper2.getTotalSpace()) {
            top = maxHeight - helper2.getTotalSpace();
        }
        scrolls = top;
        requestLayout();
    }

    @Override
    public void smoothScrollToPosition(RecyclerView recyclerView, RecyclerView.State state, int position) {//平滑的移动到某一项
        int top = layouts.get(position).top;
        int needscroll = top - scrolls;
        recyclerView.smoothScrollBy(0, needscroll);
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
```
