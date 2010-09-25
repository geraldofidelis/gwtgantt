/*
 * This file is part of gwt-gantt
 * Copyright (C) 2010  Scottsdale Software LLC
 *
 * gwt-gantt is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.

 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/
 */
package com.bradrydzewski.gwtgantt.view;

import java.util.HashMap;
import java.util.Map;

import com.bradrydzewski.gwtgantt.TaskPresenter;
import com.bradrydzewski.gwtgantt.geometry.Point;
import com.bradrydzewski.gwtgantt.geometry.Rectangle;
import com.bradrydzewski.gwtgantt.model.Task;
import com.bradrydzewski.gwtgantt.presenter.GanttWeekPresenter.Display;
import com.bradrydzewski.gwtgantt.widget.SVGDefs;
import com.bradrydzewski.gwtgantt.widget.SVGMarker;
import com.bradrydzewski.gwtgantt.widget.SVGPanel;
import com.bradrydzewski.gwtgantt.widget.SVGPath;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Style.Overflow;
import com.google.gwt.dom.client.Style.Position;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.dom.client.ScrollEvent;
import com.google.gwt.event.dom.client.ScrollHandler;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.Widget;

/**
 * 
 * @author Brad Rydzewski
 */
public abstract class GanttWeekDisplay extends Composite implements Display {

	private static GanttViewImplUiBinder uiBinder = GWT
			.create(GanttViewImplUiBinder.class);

	interface GanttViewImplUiBinder extends UiBinder<Widget, GanttWeekDisplay> {
	}


    public class TaskWidget extends FlowPanel {

        private final Task task;

        public TaskWidget(Task task) {
            this.task = task;
            sinkEvents(Event.ONCLICK | Event.ONDBLCLICK
                    | Event.ONKEYDOWN | Event.ONMOUSEOVER | Event.ONMOUSEOUT);
        }

        @Override
        public void onBrowserEvent(Event event) {

            // No need for call to super.
            switch (DOM.eventGetType(event)) {
                case Event.ONCLICK:
                    presenter.onItemClicked(task, new Point(
                    		event.getClientX(), event.getClientY()));
                    break;

                case Event.ONDBLCLICK:
                	presenter.onItemDoubleClicked(task);
                    break;

                case Event.ONKEYDOWN:
                    break;

                case Event.ONMOUSEOVER:
                    presenter.onItemMouseOver(task);
                    break;

                case Event.ONMOUSEOUT:
                    presenter.onItemMouseOut(task);
                    break;
            }
            super.onBrowserEvent(event);
        }

        public final Task getTask() {
            return task;
        }

        public Rectangle getRectangle() {
            return new Rectangle(
                    DOM.getIntStyleAttribute(this.getElement(),"left"),
                    DOM.getIntStyleAttribute(this.getElement(),"top"),
                    this.getOffsetWidth(), //+2 //HACK: add 2px to account for borders
                    this.getOffsetHeight());
        }
    }

	@UiField
	FlowPanel taskBackgroundPanel;
	@UiField
	FlowPanel taskFlowPanel;
	@UiField
	ScrollPanel taskScrollPanel;
	@UiField
	FlowPanel firstHeaderRow;
	@UiField
	FlowPanel secondHeaderRow;
	private SVGPanel svgPanel;
	private SVGDefs svgDefs;
	private SVGMarker svgArrowMarker;
	private Map<Integer, TaskWidget> taskWidgetIndex =
		new HashMap<Integer, TaskWidget>();
	
    private TaskPresenter presenter;
    private int estimatedWidth = 0;
    private int estimatedHeight = 0;

	public GanttWeekDisplay() {
		initWidget(uiBinder.createAndBindUi(this));
		init();
	}

	public void init() {
		taskScrollPanel.getElement().getStyle().setPosition(Position.ABSOLUTE);
		taskScrollPanel.getElement().getStyle().setOverflow(Overflow.SCROLL);
		
		//initialize the SVG panel
		initSVG();
		
		//add scroll handler
		taskScrollPanel.addScrollHandler(new ScrollHandler(){

			@Override
			public void onScroll(ScrollEvent event) {
				int hscroll = taskScrollPanel.getHorizontalScrollPosition()*-1;

				firstHeaderRow.getElement().getStyle().setLeft(hscroll, Unit.PX);
				secondHeaderRow.getElement().getStyle().setLeft(hscroll, Unit.PX);
				taskBackgroundPanel.getElement().getStyle().setLeft(hscroll, Unit.PX);
				
				presenter.onScroll(taskScrollPanel.getHorizontalScrollPosition(),
						taskScrollPanel.getScrollPosition());
			}
		});
	}
	
	public void initSVG() {
		svgPanel = new SVGPanel();
		svgPanel.setPointerEvents("none");
		svgPanel.setShapeRendering("crispEdges");
		svgPanel.getElement().getStyle().setOverflow(Overflow.HIDDEN);
		svgPanel.getElement().getStyle().setPosition(Position.ABSOLUTE);
		svgPanel.getElement().getStyle().setTop(0, Unit.PX);
		svgPanel.getElement().getStyle().setLeft(0, Unit.PX);

		// add the arrow marker to the svg panel
		svgArrowMarker = new SVGMarker("Triangle", .5, "black", "black");
		svgArrowMarker.setViewBox("0 0 8 8");
		svgArrowMarker.setRefX(0);
		svgArrowMarker.setRefY(4);
		svgArrowMarker.setMarkerWidth(8);
		svgArrowMarker.setMarkerHeight(8);
		svgArrowMarker.add(new SVGPath("M 0 0 L 8 4 L 0 8 z"));
		svgArrowMarker.setOrient("auto");
		svgArrowMarker.setMarkerUnits("strokeWidth");

		svgDefs = new SVGDefs();
		svgDefs.add(svgArrowMarker);
		svgPanel.add(svgDefs);
	}
    
	@Override
	public void bind(TaskPresenter presenter) {
		assert(presenter!=null);
		this.presenter = presenter;
	}
	
	@Override
	public void onBeforeRendering() {
		if(svgPanel!=null) {
			svgPanel.clear();
			svgPanel.add(svgDefs);
		}
		taskWidgetIndex.clear();
		taskFlowPanel.clear();
		firstHeaderRow.clear();
		secondHeaderRow.clear();
		estimatedWidth = 0;
		estimatedHeight = 0;
	}
	
	@Override
	public void onAfterRendering() {
		
		if(svgPanel!=null && svgPanel.getParent()==null) {
			svgPanel.getElement().getStyle().setZIndex(taskFlowPanel.getWidgetCount() + 1);
			svgPanel.getElement().getStyle().setWidth(estimatedWidth+25, Unit.PX);
			svgPanel.getElement().getStyle().setHeight(estimatedHeight, Unit.PX);
			taskFlowPanel.add(svgPanel);
		}
	}

	@Override
	public void renderTask(Task task, Rectangle bounds) {
		//add the task widget
		TaskWidget taskWidget = new TaskWidget(task);
		taskWidget.setStyleName("task");
		taskWidget.addStyleDependentName(task.getStyle());
		taskWidget.getElement().getStyle().setPosition(Position.ABSOLUTE);
		taskWidget.getElement().getStyle().setWidth(bounds.getWidth(), Unit.PX);
		taskWidget.getElement().getStyle().setHeight(bounds.getHeight(), Unit.PX);
		taskWidget.getElement().getStyle().setTop(bounds.getTop(), Unit.PX);
		taskWidget.getElement().getStyle().setLeft(bounds.getLeft(), Unit.PX);
		taskWidget.getElement().getStyle().setZIndex(taskWidgetIndex.size() + 1);
		taskWidgetIndex.put(task.getUID(), taskWidget);
		taskFlowPanel.add(taskWidget);
		
		//add the percentage complete panel
		if(task.getPercentComplete()>0) {
			SimplePanel pctCompletePanel = new SimplePanel();
			pctCompletePanel.setStyleName("pctComplete");
			pctCompletePanel.getElement().getStyle().setTop(bounds.getHeight()*.2, Unit.PX);
			pctCompletePanel.getElement().getStyle().setBottom(bounds.getHeight()*.2, Unit.PX);
			pctCompletePanel.getElement().getStyle().setLeft(0, Unit.PX);
			pctCompletePanel.getElement().getStyle().setWidth(bounds.getWidth() * task.getPercentComplete()/100, Unit.PX);
			taskWidget.add(pctCompletePanel);
		}

		//calculate the estimated size of the task area
		calculateEstimatedSize(bounds);
	}

	@Override
	public void renderTaskSummary(Task task, Rectangle bounds) {
		//add the task widget
		TaskWidget taskWidget = new TaskWidget(task);
		taskWidget.setStylePrimaryName("summary");
		taskWidget.addStyleDependentName(task.getStyle());
		taskWidget.getElement().getStyle().setPosition(Position.ABSOLUTE);
		taskWidget.getElement().getStyle().setWidth(bounds.getWidth(), Unit.PX);
		taskWidget.getElement().getStyle().setHeight(bounds.getHeight(), Unit.PX);
		taskWidget.getElement().getStyle().setTop(bounds.getTop(), Unit.PX);
		taskWidget.getElement().getStyle().setLeft(bounds.getLeft(), Unit.PX);
		taskWidget.getElement().getStyle().setZIndex(taskWidgetIndex.size() + 1);
		taskWidgetIndex.put(task.getUID(), taskWidget);
		taskFlowPanel.add(taskWidget);
		
		SimplePanel leftArrow = new SimplePanel();
		leftArrow.setStylePrimaryName("arrowLeft");
		leftArrow.addStyleDependentName(task.getStyle());
		taskWidget.add(leftArrow);
		
		SimplePanel rightArrow = new SimplePanel();
		rightArrow.setStylePrimaryName("arrowRight");
		rightArrow.addStyleDependentName(task.getStyle());
		taskWidget.add(rightArrow);

		//calculate the estimated size of the task area
		calculateEstimatedSize(bounds);
	}

	@Override
	public void renderTaskMilestone(Task task, Rectangle bounds) {
		TaskWidget taskWidget = new TaskWidget(task);

		//add the task widget
		taskWidget.setStyleName("milestone");
		taskWidget.getElement().setInnerHTML("&diams;");
		taskWidget.addStyleDependentName(task.getStyle());
		taskWidget.getElement().getStyle().setPosition(Position.ABSOLUTE);
		taskWidget.getElement().getStyle().setWidth(bounds.getWidth(), Unit.PX);
		taskWidget.getElement().getStyle().setHeight(bounds.getHeight(), Unit.PX);
		taskWidget.getElement().getStyle().setTop(bounds.getTop(), Unit.PX);
		taskWidget.getElement().getStyle().setLeft(bounds.getLeft(), Unit.PX);
		taskWidget.getElement().getStyle().setZIndex(taskWidgetIndex.size() + 1);
		taskWidgetIndex.put(task.getUID(), taskWidget);
		taskFlowPanel.add(taskWidget);
		
		//calculate the estimated size of the task area
		calculateEstimatedSize(bounds);
	}

	@Override
	public void renderTaskLabel(Task task, Rectangle bounds) {
		Label taskLabel = new Label(task.getName());
		taskLabel.setStyleName("taskLabel");
		taskLabel.getElement().getStyle().setTop(bounds.getTop(), Unit.PX);
		taskLabel.getElement().getStyle().setLeft(bounds.getLeft(), Unit.PX);
		taskFlowPanel.add(taskLabel);
	}

	@Override
	public void renderConnector(Point[] path) {
		SVGPath line = new SVGPath();
		line.setValue(path);
		line.setStroke("black");
		line.setFill("none");
		line.setStrokeWidth(1);
		line.setMarkerEnd(svgArrowMarker);

		svgPanel.add(line);
	}
	

	public void renderConnectorAsDiv(Point[] path) {
		
		Point pointA = null;
		Point pointB = null;
		int left=0, top=0, height=0, width=0;
		
		for(int i=1;i<path.length;i++) {
			
			pointA = path[i-1];
			pointB = path[i];
			
			left = Math.min(pointA.getX(), pointB.getX());
			top = Math.min(pointA.getY(), pointB.getY());
			height = Math.abs(pointB.getY()-pointA.getY());
			width = Math.abs(pointB.getX()-pointA.getX());
			
			SimplePanel div = new SimplePanel();
			div.getElement().getStyle().setBackgroundColor("black");
			div.getElement().getStyle().setPosition(Position.ABSOLUTE);
			div.getElement().getStyle().setTop(top, Unit.PX);
			div.getElement().getStyle().setLeft(left, Unit.PX);
			
			if(pointA.getX()==pointB.getX()) {
				//render a vertical line
				div.getElement().getStyle().setWidth(1, Unit.PX);
				div.getElement().getStyle().setHeight(height, Unit.PX);
			} else {
				//render a horizontal line
				div.getElement().getStyle().setHeight(1, Unit.PX);
				div.getElement().getStyle().setWidth(width, Unit.PX);
			}
			taskFlowPanel.add(div);
		}
		
		
		//Dummy code that demonstrates how we could add arrows in
		// IE. Note: it doesn't correctly render down and left arrows,
		// only right arrows at the moment
		
		//TODO: Add ability to render down and left arrows
		//TODO: Add style for arrow image
		
		SimplePanel arrowPanel = new SimplePanel();
		arrowPanel.getElement().getStyle().setPosition(Position.ABSOLUTE);
		arrowPanel.getElement().getStyle().setTop(top+height-(4), Unit.PX);
		arrowPanel.getElement().getStyle().setLeft(left+width, Unit.PX);
		arrowPanel.getElement().getStyle().setWidth(6, Unit.PX);
		arrowPanel.getElement().getStyle().setHeight(8, Unit.PX);
		arrowPanel.getElement().getStyle().setProperty("background", "transparent url(http://www.inventivetec.com/Images/arrow_black.gif) no-repeat");
		taskFlowPanel.add(arrowPanel);
	}
	
	protected void calculateEstimatedSize(Rectangle bounds) {
		estimatedHeight = Math.max(bounds.getBottom(), estimatedHeight);
		estimatedWidth = Math.max(bounds.getRight(), estimatedWidth);
	}

	@Override
	public void renderTopTimescaleCell(Rectangle bounds, String text) {
		FlowPanel weekPanel = new FlowPanel();
		weekPanel.getElement().getStyle().setWidth(bounds.getWidth(), Unit.PX);
		weekPanel.getElement().getStyle().setLeft(bounds.getLeft(), Unit.PX);
		weekPanel.add(new Label(text));
		firstHeaderRow.add(weekPanel);
	}

	@Override
	public void renderBottomTimescaleCell(Rectangle bounds, String text) {
		FlowPanel weekPanel = new FlowPanel();
		weekPanel.getElement().getStyle().setWidth(bounds.getWidth(), Unit.PX);
		weekPanel.getElement().getStyle().setLeft(bounds.getLeft(), Unit.PX);
		weekPanel.add(new Label(text));
		secondHeaderRow.add(weekPanel);
	}

	@Override
	public void renderRow(Rectangle bounds, int rowNumber) {
		// TODO Add ability to render "rows" with alternate row colors
	}

	@Override
	public void renderColumn(Rectangle bounds, int dayOfWeek) {
		FlowPanel weekendPanel = new FlowPanel();
		weekendPanel.setStyleName((dayOfWeek==0)?"weekendPanel-Sunday":"weekendPanel-Saturday");
		weekendPanel.getElement().getStyle().setWidth(bounds.getWidth(), Unit.PX);
		weekendPanel.getElement().getStyle().setLeft(bounds.getLeft(), Unit.PX);
		taskBackgroundPanel.add(weekendPanel);
	}

	@Override
	public void doTaskSelected(Task task) {
    	TaskWidget widget = taskWidgetIndex.get(task.getUID());
    	String style = "taskSelected";
    	if(task.isMilestone())
    		style="milestoneSelected";
    	else if(task.isSummary())
    		style="summarySelected";
    	widget.addStyleName(style);
	}

	@Override
	public void doTaskDeselected(Task task) {
	  	TaskWidget widget = taskWidgetIndex.get(task.getUID());
	  	String style = "taskSelected";
	  	if(task.isMilestone())
	  		style="milestoneSelected";
	  	else if(task.isSummary())
	  		style="summarySelected";
	  	widget.removeStyleName(style);
	}

	@Override
	public void doTaskEnter(Task task) {
	  	TaskWidget widget = taskWidgetIndex.get(task.getUID());
	  	String style = "taskHovered";
	  	if(task.isMilestone())
	  		style="milestoneHovered";
	  	else if(task.isSummary())
	  		style="summaryHovered";
	  	widget.addStyleName(style);
	}

	@Override
	public void doTaskExit(Task task) {
	  	TaskWidget widget = taskWidgetIndex.get(task.getUID());
	  	String style = "taskHovered";
	  	if(task.isMilestone())
	  		style="milestoneHovered";
	  	else if(task.isSummary())
	  		style="summaryHovered";
	  	widget.removeStyleName(style);
	}
	
	@Override
	public Rectangle getTaskRectangle(int UID) {
		TaskWidget taskWidget =  taskWidgetIndex.get(UID);
		if(taskWidget!=null)
			return taskWidgetIndex.get(UID).getRectangle();
		else return null;
	}


	@Override
	public int getHorizontalScrollPosition() {
		return taskScrollPanel.getHorizontalScrollPosition();
	}

	@Override
	public int getVerticalScrollPosition() {
		return taskScrollPanel.getScrollPosition();
	}

	@Override
	public Widget asWidget() {
		return this;
	}

	@Override
	public void doScroll(int x, int y) {
	
		taskScrollPanel.setHorizontalScrollPosition(x);
		taskScrollPanel.setScrollPosition(y);
	}

}
