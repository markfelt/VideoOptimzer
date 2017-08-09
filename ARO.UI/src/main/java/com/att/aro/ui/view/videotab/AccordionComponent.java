/*
*  Copyright 2017 AT&T
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*      http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/
package com.att.aro.ui.view.videotab;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.math.BigDecimal;
import java.text.MessageFormat;
import java.util.Collection;

import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.plaf.basic.BasicArrowButton;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;

import com.att.aro.core.IVideoBestPractices;
import com.att.aro.core.pojo.AROTraceData;
import com.att.aro.core.videoanalysis.pojo.AROManifest;
import com.att.aro.core.videoanalysis.pojo.VideoEvent;
import com.att.aro.ui.commonui.AROUIManager;
import com.att.aro.ui.commonui.ContextAware;
import com.att.aro.ui.commonui.IARODiagnosticsOverviewRoute;
import com.att.aro.ui.utils.ResourceBundleHelper;
import com.att.aro.ui.view.MainFrame;
import com.att.aro.ui.view.SharedAttributesProcesses;

public class AccordionComponent extends JPanel implements ActionListener {

	private static final long serialVersionUID = 1L;
	
	private JPanel hiddenPanel;
	private AROManifest aroManifest;
	private JTable jTable;
	private BasicArrowButton arrowButton;
	private IARODiagnosticsOverviewRoute diagnosticsOverviewRoute;
	private JLabel lbl;
	private AROTraceData analyzerResult;

	private SharedAttributesProcesses aroView;
	private int rowCount;
	private JScrollPane tableScrollPane;
	private JPanel titlePanel;
	private JCheckBox enableCheckBox;
	
	public AccordionComponent(AROManifest aroManifest, IARODiagnosticsOverviewRoute diagnosticsOverviewRoute, AROTraceData analyzerResult, SharedAttributesProcesses aroView) {
		this(true, aroView, aroManifest);

		this.diagnosticsOverviewRoute = diagnosticsOverviewRoute;
		this.aroManifest = aroManifest;
		this.analyzerResult = analyzerResult;
		updateHiddenPanelContent(true);
	}
	
	public void resize(int width) {

		hiddenPanel.setPreferredSize(new Dimension(width, hiddenPanel.getHeight()));
		hiddenPanel.setSize(width, hiddenPanel.getHeight());
	}
	
	public AccordionComponent(boolean manifestFlag, SharedAttributesProcesses aroView, AROManifest aroManifest) {

		this.aroView = aroView;
		this.aroManifest = aroManifest;
		setLayout(new BorderLayout());
		
		add(getTitleButton(),BorderLayout.NORTH);

		hiddenPanel = getHiddenPanel();
		add(hiddenPanel,BorderLayout.SOUTH);
		hiddenPanel.setVisible(false);
		
		arrowButton.addActionListener(this);
		updateHiddenPanelContent(false);
	}
	
	private Component getTitleButton() {

		titlePanel = new JPanel(new FlowLayout(FlowLayout.LEADING));
		titlePanel.setBackground(UIManager.getColor(AROUIManager.PAGE_BACKGROUND_KEY));
		
		arrowButton = new BasicArrowButton(SwingConstants.EAST);
		
		lbl = new JLabel();
		lbl.setFont(new Font("accordionLabel", Font.ITALIC, 12));// AROUIManager.LABEL_FONT);
		
		enableCheckBox = new JCheckBox();
		boolean selected = aroManifest.getVideoEventsBySegment() != null? true:false;		
		if ((!selected) || aroManifest.getVideoEventsBySegment().isEmpty()
				|| ((VideoEvent) aroManifest.getVideoEventsBySegment().toArray()[0]).getSegment() < 0) {
			enableCheckBox.setEnabled(false);
		} else {
			enableCheckBox.setSelected(aroManifest.isSelected());
			enableCheckBox.addActionListener(new ActionListener() {

				@Override
				public void actionPerformed(ActionEvent e) {

					if (e.getSource().getClass().equals(JCheckBox.class)) {
						aroManifest.setSelected(((JCheckBox) e.getSource()).isSelected());
						reAnalyze();
						((MainFrame) aroView).getVideoTab().openStartUpDelayWarningDialog(aroView.getTracePath());
					}
				}
			});
		}
		titlePanel.add(enableCheckBox);
		titlePanel.add(arrowButton);
		titlePanel.add(lbl);
		return titlePanel;
	}
	
	public void updateTitleButton(AROTraceData traceData) {
		if (titlePanel != null) {
			analyzerResult = traceData;
			for (AROManifest manifest : analyzerResult.getAnalyzerResult().getVideoUsage().getManifests()) {
				
				if (manifest.equals(aroManifest) 
						&& ((!aroManifest.getVideoEventsBySegment().isEmpty()) 
								&& ((VideoEvent) aroManifest.getVideoEventsBySegment().toArray()[0]).getSegment() >= 0)) {
					aroManifest.setSelected(manifest.isSelected());
					enableCheckBox.setSelected(aroManifest.isSelected());
					break;
				}
			}
		}
	}

	private IVideoBestPractices videoBestPractices = ContextAware.getAROConfigContext().getBean(IVideoBestPractices.class);

	protected void reAnalyze() {
		analyzerResult = videoBestPractices.analyze(analyzerResult);
		((MainFrame)aroView).getDiagnosticTab().getGraphPanel().setTraceData(analyzerResult);
		((MainFrame)aroView).getDiagnosticTab().getGraphPanel().refresh(analyzerResult);
		((MainFrame)aroView).getVideoTab().refreshLocal(analyzerResult);
	}

	private void updateHiddenPanelContent(boolean manifestFlag) {
		String text = "";

		if (manifestFlag) {
			if (aroManifest.getVideoEventsBySegment() != null) {
				text = (aroManifest.getVideoEventsBySegment().isEmpty()
						|| ((VideoEvent) aroManifest.getVideoEventsBySegment().toArray()[0]).getSegment() < 0)
								? MessageFormat.format(
										ResourceBundleHelper.getMessageString("videotab.invalid.manifest.name"),
										aroManifest.getVideoName())
								: MessageFormat.format(ResourceBundleHelper.getMessageString("videotab.manifest.name"),
										aroManifest.getVideoName());
			} else {
				text = MessageFormat.format(ResourceBundleHelper.getMessageString("videotab.invalid.manifest.name"),
						aroManifest.getVideoName());
			}
			lbl.setText(text + ", segment count:" + aroManifest.getSegmentEventList().size());
			hiddenPanel.add(addTable(), new GridBagConstraints(0, 2, 1, 2, 1.0, 1.0, GridBagConstraints.WEST,
					GridBagConstraints.HORIZONTAL, new Insets(10, 10, 5, 10), 0, 0));
		}
	}

	private JPanel addTable() {

		Collection<VideoEvent> videoEventList = aroManifest.getVideoEventList().values();
		rowCount = videoEventList.size();
		TableModel tableModel = new AccordionTableModel(videoEventList);
		jTable = new JTable(tableModel);
		jTable.setGridColor(Color.LIGHT_GRAY);

		JTableHeader header = jTable.getTableHeader();
		JPanel panel = new JPanel();
		tableScrollPane = new JScrollPane();
		tableScrollPane.setMinimumSize(new Dimension(this.getWidth(), 20));
		if(rowCount>20){
			tableScrollPane.setPreferredSize(new Dimension(tableScrollPane.getWidth(),400));
		}else{
			tableScrollPane.setPreferredSize(new Dimension(tableScrollPane.getWidth(),jTable.getHeight()+20));
			tableScrollPane.getBounds().setSize(tableScrollPane.getWidth(), jTable.getHeight()+20);
		}
		
		tableScrollPane.setViewportView(jTable);
		panel.setLayout(new BorderLayout());
		panel.add(header, BorderLayout.NORTH);
		panel.add(tableScrollPane, BorderLayout.CENTER);

		// Sorter for jTable
		TableRowSorter<TableModel> rowSorter = new TableRowSorter<TableModel>(jTable.getModel());
		jTable.setRowSorter(rowSorter);

		for (int column = 0; column < 7; column++) {
			rowSorter.setComparator(column, new TableSortComparator(column));
		}

		// handles double clicks on table data
		jTable.addMouseListener(new MouseAdapter() {
			public void mouseClicked(MouseEvent e) {
				if (diagnosticsOverviewRoute != null && e.getClickCount() == 2) {
					if (e.getSource() instanceof JTable) {
						int selectionIndex = ((JTable) e.getSource()).getSelectedRow();
						double segmentNo = jTable.getModel().getValueAt(selectionIndex, 0) != null
								? (Double.parseDouble(jTable.getModel().getValueAt(selectionIndex, 0).toString())) 
								: -1;
						AccordionTableModel tableModel = (AccordionTableModel) jTable.getModel();
						BigDecimal segmentNumber = BigDecimal.valueOf(segmentNo);
						if (selectionIndex > -1) {
							for (VideoEvent videoEvent : tableModel.getVideoEventCollection()) {
								if (BigDecimal.valueOf(videoEvent.getSegment()).equals(segmentNumber)) {
									diagnosticsOverviewRoute.updateDiagnosticsTab(videoEvent.getSession());
									break;
								}
							}
						}
					}
				}
			}
		});
		return panel;
	}

	@Override
	public void actionPerformed(ActionEvent e) {

		AccordionComponent accordion = (AccordionComponent) SwingUtilities.getAncestorOfClass(AccordionComponent.class, (BasicArrowButton) e.getSource());
		JPanel nestedPanel = accordion.getHiddenPanel();
		if (nestedPanel != null) {
			arrowButton = (BasicArrowButton) e.getSource();
			if (arrowButton.getDirection() == SwingConstants.EAST) {
				arrowButton.setDirection(SwingConstants.SOUTH);
				if(rowCount>20){
					tableScrollPane.setPreferredSize(new Dimension(tableScrollPane.getWidth(),400));
				}else{
					tableScrollPane.setPreferredSize(new Dimension(tableScrollPane.getWidth(),jTable.getHeight()+20));
					tableScrollPane.getBounds().setSize(tableScrollPane.getWidth(), jTable.getHeight()+20);
				}
				nestedPanel.updateUI();

				nestedPanel.setVisible(true);
			} else {
				arrowButton.setDirection(SwingConstants.EAST);
				nestedPanel.setVisible(false);
			}
			aroView.getCurrentTabComponent().revalidate();
			nestedPanel.setVisible(!(arrowButton.getDirection() == SwingConstants.EAST));
		}
	}

	public void setVisible(boolean state) {

		hiddenPanel.setVisible(state);
		if (state) {
			arrowButton.setDirection(SwingConstants.SOUTH);
		} else {
			arrowButton.setDirection(SwingConstants.EAST);
		}
	}
	
	private JPanel getHiddenPanel() {
		if (hiddenPanel == null) {
			hiddenPanel = new JPanel(new GridBagLayout());
			hiddenPanel.setBackground(UIManager.getColor(AROUIManager.PAGE_BACKGROUND_KEY));
			hiddenPanel.setBorder(BorderFactory.createEtchedBorder(WIDTH));
		}
		return hiddenPanel;
	}
}