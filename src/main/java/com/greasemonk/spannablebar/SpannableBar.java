package com.greasemonk.spannablebar;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.*;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.RoundRectShape;
import android.os.Build;
import android.support.annotation.IntRange;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.widget.Toast;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Created by Wiebe Geertsma on 10-11-2016.
 * E-mail: <a href="mailto:e.w.geertsma@gmail.com?SUBJECT=SpannableBar">e.w.geertsma@gmail.com</a>
 * 
 * @see <a href="https://github.com/GreaseMonk/SpannableBar">SpannableBar on Github</a>
 * @see <a href="https://github.com/GreaseMonk/SpannableBar/issues">Issue tracker</a>
 * <br><br>
 * SpannableBar is a Grid-style spannable bar, that is useful when you need a way to span a bar over columns. 
 * The view allows you to set the starting column, the span, the number of columns, and more.
 */
public class SpannableBar extends View
{
	public static final int DEFAULT_START = 0;
	public static final int DEFAULT_SPAN = 7;
	public static final int DEFAULT_COLUMN_COUNT = 7; // week view, 7 days
	public static final float DEFAULT_RADIUS = 8f;
	public static final int DEFAULT_BAR_COLOR = Color.argb(128, 63,81,181);
	public static final int DEFAULT_TEXT_SIZE_SP = 12;
	public static final int DEFAULT_TEXT_COLOR = Color.WHITE;
	
	private String text;
	private int start = DEFAULT_START,
			span = DEFAULT_SPAN,
			columnCount = DEFAULT_COLUMN_COUNT,
			textColor = DEFAULT_TEXT_COLOR,
			color = DEFAULT_BAR_COLOR,
			textSize = DEFAULT_TEXT_SIZE_SP;
	private float scaledDensity, radius;
	private Paint textPaint, linePaint;
	private ShapeDrawable drawable;
	private boolean drawCells = false;
	private Map<Integer, Paint> columnColors;
	
	/**
	 * An array of 8 radius values, for the outer roundrect.
	 * The first two floats are for the top-left corner (remaining pairs correspond clockwise).
	 * For no rounded corners on the outer rectangle, pass null.
	 *
	 * @see <a href="https://developer.android.com/reference/android/graphics/drawable/shapes/RoundRectShape.html">RoundRectShape</a>
	 */
	private float[] radii = {
			radius, radius,
			radius, radius,
			radius, radius,
			radius, radius};
	
	
	//region CONSTRUCTORS
	
	public SpannableBar(Context context)
	{
		super(context);
		init(context, null);
	}
	
	public SpannableBar(Context context, @Nullable AttributeSet attrs)
	{
		super(context, attrs);
		init(context, attrs);
	}
	
	public SpannableBar(Context context, @Nullable AttributeSet attrs, int defStyleAttr)
	{
		super(context, attrs, defStyleAttr);
		init(context, attrs);
	}
	
	@TargetApi(Build.VERSION_CODES.LOLLIPOP)
	public SpannableBar(Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes)
	{
		super(context, attrs, defStyleAttr, defStyleRes);
		init(context, attrs);
	}
	
	//endregion
	
	
	private void init(Context context, AttributeSet attrs)
	{
		if (attrs != null)
		{
			TypedArray typedArray = context.getTheme().obtainStyledAttributes(
					attrs,
					R.styleable.SpannableBar,
					0, 0);
			try
			{
				text = typedArray.getString(R.styleable.SpannableBar_barText);
				color = typedArray.getColor(R.styleable.SpannableBar_barColor, DEFAULT_BAR_COLOR);
				textColor = typedArray.getColor(R.styleable.SpannableBar_barTextColor, Color.WHITE);
				textSize = typedArray.getDimensionPixelSize(R.styleable.SpannableBar_barTextSize, DEFAULT_TEXT_SIZE_SP);
				start = Math.abs(typedArray.getInteger(R.styleable.SpannableBar_barStart, DEFAULT_START));
				span = Math.abs(typedArray.getInteger(R.styleable.SpannableBar_barSpan, DEFAULT_SPAN));
				columnCount = Math.abs(typedArray.getInteger(R.styleable.SpannableBar_barColumns, DEFAULT_COLUMN_COUNT));
				
			} finally
			{
				typedArray.recycle();
			}
		}
		
		correctValues();
		columnColors = new HashMap<Integer, Paint>();
		scaledDensity = context.getResources().getDisplayMetrics().scaledDensity;
		Typeface typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL);
		
		textPaint = new Paint();
		textPaint.setColor(textColor);
		textPaint.setTextSize(scaledDensity * textSize);
		textPaint.setTextAlign(Paint.Align.CENTER);
		textPaint.setTypeface(typeface);
		textPaint.setAntiAlias(true);
		
		linePaint = new Paint();
		linePaint.setColor(Color.GRAY);
		linePaint.setAlpha(125);
		
		setOnClickListener(new OnClickListener()
		{
			@Override
			public void onClick(View view)
			{
				if(text != null)
					Toast.makeText(getContext(), text, Toast.LENGTH_SHORT).show();
			}
		});
		
		setRadius(DEFAULT_RADIUS);
		requestLayout();
	}
	
	protected void onDraw(Canvas canvas)
	{
		super.onDraw(canvas);
		final int colWidth = canvas.getWidth() / columnCount;
		
		if (span > 0)
		{
			if(drawCells)
			{
				// Draw the grid for this row
				// Draw a line along the bottom border
				// canvas.drawLine(0, getHeight()-2, getWidth(), getHeight(), linePaint);
				// Draw the columns
				for (int i = 0; i <= columnCount; i++)
				{
					int x = i * colWidth;
					canvas.drawLine(x, 0, x, getHeight(), linePaint);
				}
			}
			
			final int coordLeft = getPaddingLeft() + (start * colWidth);
			final int coordTop = getPaddingTop();
			final int coordRight = coordLeft + (span * colWidth) - getPaddingRight() - getPaddingLeft();
			final int coordBottom = canvas.getHeight() - getPaddingBottom();
			// Draw the column background colors
			if(columnColors != null)
			{
				for (Integer key : columnColors.keySet())
				{
					// Get coordinates without padding
					int left = key * colWidth;
					int top = 0;
					int right = left + colWidth;
					int bottom = canvas.getHeight();
					
					canvas.drawRect(left, top, right, bottom, columnColors.get(key));
				}
			}
			drawable.getPaint().setColor(color);
			drawable.setBounds(coordLeft, coordTop, coordRight, coordBottom);
			drawable.draw(canvas);
			
			
			
			// Only make a drawcall if there is actually something to draw.
			if(text != null && !text.isEmpty())
			{
				final int textCoordX = coordLeft + (coordRight / 2);
				final int textBaselineToCenter = Math.abs(Math.round(((textPaint.descent() + textPaint.ascent()) / 2)));
				final int textBaselineCoordY = (canvas.getHeight() / 2) + textBaselineToCenter;
				if (text != null && !text.isEmpty())
				{
					canvas.drawText(text, textCoordX, textBaselineCoordY, textPaint);
				}
			}
		}
	}
	
	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec)
	{
		int desiredWidth = 100;
		int desiredHeight = 100;
		
		int widthMode = MeasureSpec.getMode(widthMeasureSpec);
		int widthSize = MeasureSpec.getSize(widthMeasureSpec);
		int heightMode = MeasureSpec.getMode(heightMeasureSpec);
		int heightSize = MeasureSpec.getSize(heightMeasureSpec);
		
		int width;
		int height;
		
		// Measure width
		if (widthMode == MeasureSpec.EXACTLY)
			width = widthSize;
		else if (widthMode == MeasureSpec.AT_MOST)
			width = Math.min(desiredWidth, widthSize);
		else
			width = desiredWidth;
		
		// Measure height
		if (heightMode == MeasureSpec.EXACTLY)
			height = heightSize;
		else if (heightMode == MeasureSpec.AT_MOST)
			height = Math.min(desiredHeight, heightSize);
		else
			height = desiredHeight;
		
		setMeasuredDimension(width, height);
	}
	
	/**
	 * Correct the start, span, and column count where needed.
	 */
	private void correctValues()
	{
		// Make sure to set values to zero if it was set below that.
		start = Math.max(0, start);
		span = Math.max(0, span);
		columnCount = Math.max(0, columnCount);
		if(columnColors != null)
		{
			Iterator<Integer> iterator = columnColors.keySet().iterator();
			while(iterator.hasNext())
			{
				Integer key = iterator.next();
				if(key > columnCount)
					columnColors.remove(key);
			}
		}
		
		
		// Make sure the span vale is correct.
		if(start + span > columnCount)
		{
			if(start <= columnCount)
				span = columnCount - start;
			if(start >= columnCount)
				start = columnCount - 1;
		}
	}
	
	//region GETTERS & SETTERS
	
	/**
	 * Set a column's cell background color.
	 * 
	 * @param row the row to apply the color to
	 * @param color the color to apply 
	 */
	public void setColumnColor(int row, int color)
	{
		Paint paint = new Paint();
		paint.setColor(color);
		columnColors.put(row, paint);
	}
	
	/**
	 * Removes all coloring that was previously applied to any column.
	 */
	public void removeColumnColors()
	{
		columnColors.clear();
	}
	
	/**
	 * Removes the column color of a specific row.
	 * 
	 * @param row the row to remove the column color of.
	 */
	public void removeColumnColor(int row)
	{
		if(columnColors == null)
			return;
		
		if(columnColors.containsKey(row))
			columnColors.remove(row);
	}
	
	/**
	 * Sets all the required properties of the bar in one go.
	 * Any values will be corrected for you, for example:
	 * start = 3, span = 7, columnCount = 7, will have it's span corrected to 4.
	 * Any values below zero will be automatically corrected to zero.
	 * 
	 * 
	 * @param start the start column of the bar (0 to columnCount)
	 * @param span the span of the bar
	 * @param columnCount the amount of columns to set
	 */
	public void setProperties(@IntRange(from=0) int start, @IntRange(from=0) int span, @IntRange(from=1) int columnCount)
	{
		this.start = start;
		this.span = span;
		this.columnCount = columnCount;
		correctValues();
		invalidate();
	}
	
	/**
	 * Set the amount of columnCount
	 *
	 * @param numColumns the amount of columnCount to set
	 */
	public void setColumnCount(@IntRange(from=0) int numColumns)
	{
		columnCount = numColumns;
		correctValues();
		invalidate();
	}
	
	/**
	 * Set the displayed text. The view will automatically be invalidated.
	 *
	 * @param text the text to be displayed
	 */
	public void setText(@Nullable String text)
	{
		this.text = text == null ? "" : text;
		invalidate();
	}
	
	/**
	 * Set the desired starting column of the bar. Any amount that is higher than the span
	 * will automatically limit itself to the value of columnCount.
	 * If you have set the amount of columns to 7, use 0-6.
	 *
	 * @param start the start column of the bar (0 to columnCount)
	 */
	public void setStart(@IntRange(from=0) int start)
	{
		this.start = start;
		correctValues();
		invalidate();
	}
	
	/**
	 * Set the bar's span. This is the actual span, 
	 * so a value of 1 will show a bar with one column filled.
	 *
	 * @param span the span to set the bar to.
	 */
	public void setSpan(@IntRange(from=0) int span)
	{
		this.span = span;
		correctValues();
		invalidate();
	}
	
	/**
	 * Set the bar's corner radius
	 *
	 * @param radius the radius to set
	 */
	public void setRadius(float radius)
	{
		this.radius = scaledDensity * radius + 0.5f;
		this.radii = new float[]{
				radius, radius,
				radius, radius,
				radius, radius,
				radius, radius
		};
		drawable = new ShapeDrawable(new RoundRectShape(radii, null, null));
		invalidate();
	}
	
	/**
	 * Set the bar text size. Values that are zero or below will be brought back up to 1.
	 *
	 * @param sp
	 */
	public void setTextSize(@IntRange(from=1) int sp)
	{
		this.textSize = Math.max(1, sp);
		textPaint.setTextSize(scaledDensity * this.textSize);
		invalidate();
	}
	
	/**
	 * Set the bar color
	 * 
	 * @param color the color to set the bar to, such as Color.WHITE.
	 */
	public void setColor(int color)
	{
		this.color = color;
		invalidate();
	}
	
	/**
	 * Set the text alignment
	 * 
	 * @param align the alignment of the text to set.
	 */
	public void setTextAlignment(@NonNull Paint.Align align)
	{
		textPaint.setTextAlign(align);
		invalidate();
	}
	
	/**
	 * Shows additional lines along the outline of each cell.
	 * 
	 * @param show set to TRUE to show cell lines
	 */
	public void setShowCellLines(boolean show)
	{
		this.drawCells = show;
		invalidate();
	}
	
	/**
	 * Set the cell lines color
	 * 
	 * @param color the color to set the cell lines to.
	 */
	public void setCellLineColor(int color)
	{
		this.linePaint.setColor(color);
		invalidate();
	}
	
	/**
	 * Set the text typeface, to set the desired font.
	 * Default: Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD);
	 * 
	 * @param typeface the typeface to assign to the text
	 */
	public void setTextTypeface(@NonNull Typeface typeface)
	{
		textPaint.setTypeface(typeface);
		invalidate();
	}
	
	//endregion
}
