/**
 * @(#)JCalendar.java	0.1 28/06/2002
 *
 * Copyright (c) 2002 Arron Ferguson
 *
 */

package ca.bcit.geekkit;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.Toolkit;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;

import java.lang.reflect.Constructor;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.SwingConstants;

import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableModel;

/**
 * <p><code>JCalendar</code> is a complex widget that keeps track of a calendar. It uses its own
 * customized <code>TableModel</code> for keeping track of specific dates that may require custom
 * rendrering other than the usual number being in each cell.</p>
 *
 * @version	0.1 28/06/2002
 * @author 	Arron Ferguson
 */
public class JCalendar extends JPanel implements ActionListener
{
	/**
	 * For clicking on to take the calendar to the next month
	 */
	private JButton nextButton;

	/**
	 * For clicking on to take the calendar to the previous month
	 */
	private JButton previousButton;

	/**
	 * Displays the month and the year inside of the calendar layout
	 */
	protected JLabel label;

	/**
	 * The set of rows and columns used to display dates.
	 */
	protected JTable table;

	/**
	 * Layout that allows for a grid like layout pattern. Components do not have to
	 * take up exactly one cell, instead they can take up more than one row or column.
	 */
	private GraphPaperLayout gp;

	/**
	 * A custom <code>TableModel</code> for dealing with specifically calendar like cells
	 */
	private CalendarTableModel model;

	private Class tableClass;

	/**
	 * Default constructor
	 */

	public JCalendar()
	{	this( JCalendarTable.class );
	}

	public JCalendar( Class tableClass )
	{
		super();

		this.tableClass = tableClass;

		configUI();

		nextButton.addActionListener(this);
		previousButton.addActionListener(this);
	}

	/**
	 * Configures the UI and sets up the renderers
	 */

	private void configUI()
	{
		gp = new GraphPaperLayout(new Dimension(8, 10));
		setLayout(gp);
		nextButton = new JButton("Next");
		previousButton = new JButton("Back");
		label = new JLabel("", JLabel.CENTER);

		model = new CalendarTableModel(this);
		initializeTable();

		table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);

		// don't allow rows or columns to be selected
		table.setCellSelectionEnabled(true);
		table.setColumnSelectionAllowed(false);
		table.setRowSelectionAllowed(false);

		// don't allow cells to be selected
		table.setFocusable(true);

		// little bit of customization of cell renderers
		JLabel cell = (JLabel)table.getDefaultRenderer(JLabel.class);
		cell.setHorizontalAlignment(SwingConstants.LEFT);
		cell.setVerticalAlignment(SwingConstants.TOP);

		table.getTableHeader().setReorderingAllowed(false);
		// add buttons
		add(previousButton, new Rectangle(0, 0, 2, 1));
		add(nextButton, new Rectangle(6, 0, 2, 1));
		// add label
		add(label, new Rectangle(2, 0, 4, 1));
		add(table.getTableHeader(), new Rectangle(0, 2, 8, 1));
		add(table, new Rectangle(0, 3, 8, 7));

		// now call it for a populate
		model.generateCalendarMonth(0);
	}

	/**
	 * Handles the two <code>JButton</code>s events for going forward and backward
	 * in the years
	 *
	 * @param e the <code>ActionEvent</code> given.
	 */
	public void actionPerformed(ActionEvent e)
	{
		if(e.getSource() == nextButton)
		{
			model.generateCalendarMonth(1);
		} else if(e.getSource() == previousButton)
		{
			model.generateCalendarMonth(-1);
		}
	}

	/**
	 * Custom paint to allow cells to change height based on the size of the <code>Container</code>
	 * that the table is in
	 *
	 * @param g the <code>Graphics</code> context used to paint the table.
	 */
	public void paint(Graphics g)
	{
		super.paint(g);

		// make row height resize as the component is resized so that rows fill up the space
		float componentHeight = (float)table.getHeight();
		float numberofRows = (float)table.getRowCount();
		float tableCellHeight = (componentHeight / numberofRows);
		int height = (int)tableCellHeight;
		table.setRowHeight(height);
	}

	/**
	 * Returns the preferred size of this composite component
	 *
	 * @return the width and height of this component as a <code>Dimension</code> object and as
	 * its preferred size to be rendered.
	 */
	public Dimension getPreferredSize()
	{
		return new Dimension(310, 220);
	}

	/**
	 * Returns the minimum size that this composite component should be drawn at
	 * @return the minimum width and height that this component should be rendered at
	 */
	public Dimension getMinimumSize()
	{
		return new Dimension(260, 170);
	}

	public JTable getTable()
	{	return table;
	}

	public CalendarTableModel getModel()
	{	return model;
	}

	private void initializeTable()
	{
		try
		{
			if ( JTable.class.isAssignableFrom( tableClass ) )
			{
				Object [] parameters = new Object[1];
				parameters[0] = model;

				Class [] parameterTypes = new Class[1];
				parameterTypes[0] = CalendarTableModel.class;

				table = (JTable) tableClass.getConstructor( parameterTypes ).newInstance( parameters );
			}
		}
		catch ( Exception e )
		{
		}

		if ( table == null )
			table = new JCalendarTable( model );
	}

	public class JCalendarTable extends JTable
	{
		private DefaultTableCellRenderer highlighter;

		public JCalendarTable( CalendarTableModel model )
		{
			super( model );

			highlighter = new DefaultTableCellRenderer();
			highlighter.setForeground( new Color( 255, 255, 255 ) );
			highlighter.setBackground( new Color( 0, 0, 128 ) );
		}

		public TableCellRenderer getCellRenderer( int row, int column )
		{
			if ( String.valueOf(model.getCurrentDate()).equals(model.getValueAt(row, column)) &&
				model.getCurrentMonth() == model.getMonth() && model.getCurrentYear() == model.getYear() )
			{
				return highlighter;
			}

			return super.getCellRenderer(row, column);
		}
	}

	/**
	 * For running this program
	 */
	public static void main(String[] args)
	{
		JCalendar jc = new JCalendar();
		JFrame frame = new JFrame("calendar");
		frame.getContentPane().add(jc);
		Dimension frameD = new Dimension(310, 220);
		Dimension screenD = new Dimension();
		screenD = Toolkit.getDefaultToolkit().getScreenSize();
		if(frameD.width >= screenD.width)
			frame.setLocation(1, 1);
		frame.setLocation(((screenD.width - frameD.width)/2), ((screenD.height - frameD.height)/2));
		frame.setSize(frameD.width, frameD.height);
		frame.addWindowListener(new WindowAdapter()
			{
				public void windowClosing(WindowEvent e)
				{
				System.exit(0);
					}
			}
		);
		frame.setVisible(true);
	}
}
