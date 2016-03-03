package drafty.views;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Pattern;
import java.util.HashSet;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.sql.DataSource;

import org.vaadin.viritin.util.BrowserCookie;

import com.vaadin.data.Item;
import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.data.Property.ValueChangeListener;
import com.vaadin.data.util.GeneratedPropertyContainer;
import com.vaadin.data.util.IndexedContainer;
import com.vaadin.data.util.filter.SimpleStringFilter;
import com.vaadin.data.validator.EmailValidator;
import com.vaadin.data.validator.StringLengthValidator;
import com.vaadin.event.FieldEvents.BlurEvent;
import com.vaadin.event.FieldEvents.BlurListener;
import com.vaadin.event.FieldEvents.FocusEvent;
import com.vaadin.event.FieldEvents.FocusListener;
import com.vaadin.event.FieldEvents.TextChangeEvent;
import com.vaadin.event.FieldEvents.TextChangeListener;
import com.vaadin.event.ItemClickEvent;
import com.vaadin.event.ItemClickEvent.ItemClickListener;
import com.vaadin.event.SortEvent;
import com.vaadin.event.SortEvent.SortListener;
import com.vaadin.navigator.View;
import com.vaadin.navigator.ViewChangeListener;
import com.vaadin.navigator.ViewChangeListener.ViewChangeEvent;
import com.vaadin.server.FontAwesome;
import com.vaadin.server.Page;
import com.vaadin.server.Responsive;
import com.vaadin.server.WebBrowser;
import com.vaadin.shared.ui.grid.HeightMode;
import com.vaadin.shared.ui.label.ContentMode;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.CssLayout;
import com.vaadin.ui.Grid;
import com.vaadin.ui.Grid.HeaderCell;
import com.vaadin.ui.Grid.HeaderRow;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.MenuBar;
import com.vaadin.ui.MenuBar.MenuItem;
import com.vaadin.ui.Notification;
import com.vaadin.ui.Panel;
import com.vaadin.ui.TextArea;
import com.vaadin.ui.TextField;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Window;
import com.vaadin.ui.themes.ValoTheme;

import drafty.services.InteractionService;
import drafty.services.MailService;
import drafty.widgets.SuggestionComponent;

public class Profs extends VerticalLayout implements View {
	
	private static final long serialVersionUID = -6955613369737022454L;
	String DATASOURCE_CONTEXT = _MainUI.getDataProvider().getJNDI();
	
	//set Drafty cookie value
	private String cookieCheck = "brown_university_drafty_cookie";
	private String cookieValue = "0";
	private WebBrowser webBrowser = Page.getCurrent().getWebBrowser();
	private String browser;
	private String browserNumber = Integer.toString(webBrowser.getBrowserMajorVersion()) + "." + Integer.toString(webBrowser.getBrowserMinorVersion());
	private String ipAddress = webBrowser.getAddress();
	private String idProfile = null;
	private String idIpAddress = null;
	
	private Integer flag_sugg = 0; //for menu button
	private String icono = "<span class=\"v-menubar-menuitem-caption\" style=\"color:#0095da\"><span class=\"v-icon FontAwesome\"></span>Select cell to make a Suggestion</span>";
	private String icono2 = "<span class=\"v-menubar-menuitem-caption\" style=\"color:#0095da\"><span class=\"v-icon FontAwesome\"></span>Click here to make a Suggestion</span>";
	
	VerticalLayout mainLayout = new VerticalLayout();
	protected final MenuBar draftyMenu = new MenuBar();
	
	HorizontalLayout leftH = new HorizontalLayout();
	HorizontalLayout rightH = new HorizontalLayout();
	HorizontalLayout cookieMenu = new HorizontalLayout();
	
	Label label_headingL = new Label("<h3>Computer Science Professors</h3>", ContentMode.HTML);
	Label label_headingR = new Label("<h5>Brown University HCI Research Group</h5>", ContentMode.HTML);
	Label label_badges = new Label();
	Label label_badges_info = new Label("Suggest new data or validate existing data to earn more badges!");
	
	HorizontalLayout horLay1 = new HorizontalLayout();
	HorizontalLayout horLay2 = new HorizontalLayout();
	
	CssLayout panelWrap = new CssLayout();
	
	Panel resultsPanel = new Panel();
	VerticalLayout resultsPanelLayout = new VerticalLayout();
	
	IndexedContainer container = new IndexedContainer();
	Grid resultsGrid = new Grid();
	
	MenuItem suggestionMode = null;
	
	private String cell_id;
	private String cell_full_name;
	private String cell_value;
	private String cell_column;
	
	//modal for email / contact
	private Window subMail = new Window();
	private TextField fName = new TextField("First Name", "");
	private TextField lName = new TextField("Last Name", "");
	private TextField email = new TextField("Email", "");
	private TextArea message = new TextArea("Message", "");
	private Button submitEmail = new Button("Send");
	
	public Profs() {
		detectBrowser();
		detectCookie(); //first set check and set cookie value
		checkHostName();
		
		buildMenu();
		
		addContactValidators();
		
		buildGrid();
		
		addFilters();

		populateGrid("<= 32"); //only 20 inital row
		resultsGrid.sort("University");
		resultsGrid.removeColumn("id");
	}
	
	@SuppressWarnings("serial")
	private void addContactValidators() {
		fName.setBuffered(true);
		fName.addValidator(new StringLengthValidator("The name must be 1-100 characters (input: {0})", 1, 100, true));
		fName.setNullSettingAllowed(false);
		fName.setValidationVisible(true);
		fName.addValueChangeListener(new ValueChangeListener() {
			@Override
			public void valueChange(ValueChangeEvent event) { contactFieldValueChange(); } /* reads all value changes */
		});
		
		lName.setBuffered(true);
		lName.addValidator(new StringLengthValidator("The name must be 1-100 characters (input: {0})", 1, 100, true));
		lName.setNullSettingAllowed(false);
		lName.setValidationVisible(true);
		lName.addValueChangeListener(new ValueChangeListener() {
			@Override
			public void valueChange(ValueChangeEvent event) { contactFieldValueChange(); } /* reads all value changes */
		});
		
		email.setBuffered(true);
		email.addValidator(new StringLengthValidator("The name must be 1-150 characters (input: {0})", 1, 150, true));
		email.setNullSettingAllowed(false);
		email.setValidationVisible(true);
		email.addValidator(new EmailValidator("Please enter a valid email address"));
		email.addValueChangeListener(new ValueChangeListener() {
			@Override
			public void valueChange(ValueChangeEvent event) { contactFieldValueChange(); } /* reads all value changes */
		});
		
		message.setBuffered(true);
		message.addValidator(new StringLengthValidator("The name must be 1-1500 characters (input: {0})", 1, 1500, true));
		message.setNullSettingAllowed(false);
		message.setValidationVisible(true);
		message.addValueChangeListener(new ValueChangeListener() {
			@Override
			public void valueChange(ValueChangeEvent event) { contactFieldValueChange(); } /* reads all value changes */
		});
	}
	
	private void contactFieldValueChange() {
		/* reads all value changes */
		if (fName.isValid() && lName.isValid() && email.isValid() && message.isValid()) {
			submitEmail.setEnabled(true);
		} else {
			submitEmail.setEnabled(false);
		}
	}
	
	private void checkHostName(){ 
		InetAddress addr = null;
		try {
			addr = InetAddress.getByName(ipAddress);
		} catch (UnknownHostException e) {
			System.out.println("Cannot resolve host name");
		}
		if (addr != null){
			String domainName = addr.getCanonicalHostName();
			System.out.println("Domain Name: " + domainName);
			String strippedName = stripName(domainName);
			System.out.println("Stripped Domain Name:" + strippedName);
			HashMap<String,String> uniHash = _MainUI.getDataProvider().getDomains();
			//System.out.println(uniHash);
			if (uniHash.containsKey(strippedName)){
				System.out.println("The domain name matches a university: " + uniHash.get(strippedName));
				//TO-DO - updateProfile(strippedName);
			}
		}
	}
	
	private String stripName(String s){
		String[] split = s.split(Pattern.quote("."));		
		if (split.length >= 2){
			String extension = split[split.length-1].toLowerCase();
			String domain = split[split.length-2].toLowerCase(); 
			s = domain.concat(".").concat(extension); 
		}
		return s;
	}
	
	private void updateBadges() {
		Integer count = 0;
		
		for(MenuItem mi : draftyMenu.getItems()) {
			if(mi.getText().equals("Badges")) {		
				try {
			      Context initialContext = new InitialContext();
			      DataSource datasource = (DataSource)initialContext.lookup(DATASOURCE_CONTEXT);
			      
			      if (datasource != null) {
			        Connection conn = datasource.getConnection();
			        String sql = 
			        		"SELECT COUNT(s.Suggestion) as count "
			        		+ "FROM Suggestion s "
			        		+ "WHERE s.idProfile = " + idProfile;
			        PreparedStatement stmt = conn.prepareStatement(sql);
			        try {
			        	ResultSet rs = stmt.executeQuery();
						while (rs.next()) {
							count = rs.getInt("count");
						}
			        } catch (SQLException e) {
						System.out.println(e.getMessage());
					}
			        sql = 
			        		"SELECT COUNT(v.idValidation) as count "
			        		+ "FROM Validation v "
			        		+ "WHERE v.idProfile = " + idProfile;
			        stmt = conn.prepareStatement(sql);
			        try {
			        	ResultSet rs = stmt.executeQuery();
						while (rs.next()) {
							count = count + rs.getInt("count");
						}
			        } catch (SQLException e) {
						System.out.println(e.getMessage());
					}
			        stmt.close();
			        conn.close();
			      }
			    }
		        catch (Exception ex)
		        {
		        	System.out.println("Exception getBadges() " + ex);
		        }	
				
				String badge_info = "<p class=\"projectinfo\">You have earned the ";
				if (count == 0) {
					label_badges.setValue(badge_info + "Unhappy badge. " + FontAwesome.FROWN_O.getHtml() +  "  Make a suggestion and turn that frown upside down. :) ");
				} else if (count < 5) {
					label_badges.setValue(badge_info + "Happy badge. " + FontAwesome.SMILE_O.getHtml() +  "  Thank you for making a suggestion! Keep going to upgrade your badge. ");
				} else if (count < 8) {
					label_badges.setValue(badge_info + "Thumbs Up badge. " + FontAwesome.THUMBS_UP.getHtml() +  "  Great job you are a true friend!  ");
				} else if (count < 12) {
					label_badges.setValue(badge_info + "Gamer badge. " + FontAwesome.GAMEPAD.getHtml() +  "  Excellent, you must have amazing skills.  Keep going!  ");
				} else if (count < 17) {
					label_badges.setValue(badge_info + "Rebel Alliance badge. " + FontAwesome.REBEL.getHtml() +  "  The force is strong with you!  ");
				} else if (count >= 17) {
					label_badges.setValue(badge_info + "Galactic Empire badge. " + FontAwesome.GE.getHtml() +  "  You are the most dominant force in the galaxy!  ");
				}
			}
		}
	}
	
	@SuppressWarnings("serial")
	private void buildGrid() {
		container.addContainerProperty("id", String.class, null);
		container.addContainerProperty("FullName", String.class, null);
		container.addContainerProperty("University", String.class, null);
		container.addContainerProperty("JoinYear", String.class, null);
		container.addContainerProperty("Rank", String.class, null);
		container.addContainerProperty("Subfield", String.class, null);
		container.addContainerProperty("Bachelors", String.class, null);
		container.addContainerProperty("Masters", String.class, null);
		container.addContainerProperty("Doctorate", String.class, null);
		container.addContainerProperty("PostDoc", String.class, null);
		container.addContainerProperty("Gender", String.class, null);
		container.addContainerProperty("PhotoUrl", String.class, null);
		container.addContainerProperty("Sources", String.class, null);
		
		GeneratedPropertyContainer gpcontainer = new GeneratedPropertyContainer(container);
		
		resultsGrid.setContainerDataSource(gpcontainer);
		resultsGrid.setSelectionMode(Grid.SelectionMode.NONE);
		
		//Gets designated column value from row selection 
		resultsGrid.addItemClickListener(new ItemClickListener() {
            @Override
            public void itemClick(ItemClickEvent e) {
            	//System.out.println("Click Name: " + (String) e.getItem().getItemProperty("FullName").getValue());
            	
            	flag_sugg = 1;
            	suggestionMode.setText(icono2);
            	
            	cell_id = (String) e.getItem().getItemProperty("id").getValue();
				cell_full_name = (String) e.getItem().getItemProperty("FullName").getValue();
				cell_value = container.getContainerProperty(e.getItemId(), e.getPropertyId()).getValue().toString();
				cell_column = e.getPropertyId().toString();
            	
            	InteractionService is = new InteractionService();
				
                if (e.isDoubleClick()) {
                	is.recordClick(cell_id, cell_full_name, cell_value, cell_column, "1", idProfile); //1 to record it as double click
                	if(cell_column.equals("FullName")) {
                		Notification.show("Full Name is not available to make Suggestions");
                	} else {
                		new SuggestionComponent(cell_id, cell_full_name, cell_value, cell_column, idProfile);
                	}
                } else { //single click
                	is.recordClick(cell_id, cell_full_name, cell_value, cell_column, "0", idProfile);
                }
            }
        });
		
		resultsGrid.addSortListener(new SortListener() {

			@Override
			public void sort(SortEvent event) {
				resetSuggestionMenuItem();
			}
		});
		
		setSpacing(false);
		setMargin(false);
		
		// main layout wrapper
		mainLayout.setWidth("100%");
		this.addComponent(mainLayout);
		mainLayout.addStyleName("main-wrap");
		Responsive.makeResponsive(mainLayout);
		
		// panel & panelLayout
		mainLayout.addComponent(panelWrap);
		panelWrap.setWidth("100%");
		panelWrap.setHeight("100%");
		panelWrap.addStyleName("panelWrap");
		Responsive.makeResponsive(panelWrap);
		panelWrap.addStyleName("panel-padding");
		
		//resultsPanelLayout.addComponent(resultsGrid);
		panelWrap.addComponent(resultsGrid);
		resultsGrid.setColumnReorderingAllowed(true);
		resultsGrid.setWidth("100%");
	    //resultsGrid.setHeightMode(HeightMode.ROW);
	    //resultsGrid.setHeightByRows(15);
	    resultsGrid.setHeightMode(HeightMode.CSS);
		resultsGrid.setHeight((Page.getCurrent().getWebBrowser().getScreenHeight() - 200), Unit.PIXELS);
	    
	    //Set Column header names
	    resultsGrid.getColumn("id").setHeaderCaption("ID");
		resultsGrid.getColumn("FullName").setHeaderCaption("Name").setExpandRatio(0);
		resultsGrid.getColumn("University").setHeaderCaption("University").setWidth(200);
		resultsGrid.getColumn("JoinYear").setHeaderCaption("Join Year").setWidth(70);
		resultsGrid.getColumn("Rank").setHeaderCaption("Rank").setWidth(70);
		resultsGrid.getColumn("Subfield").setHeaderCaption("Subfield").setWidth(200);
		resultsGrid.getColumn("Bachelors").setHeaderCaption("Bachelors").setExpandRatio(0);
		resultsGrid.getColumn("Masters").setHeaderCaption("Masters");
		resultsGrid.getColumn("Doctorate").setHeaderCaption("Doctorate");
		resultsGrid.getColumn("PostDoc").setHeaderCaption("PostDoc");
		resultsGrid.getColumn("Gender").setHeaderCaption("Gender").setWidth(70);
		resultsGrid.getColumn("PhotoUrl").setHeaderCaption("PhotoUrl");
		resultsGrid.getColumn("Sources").setHeaderCaption("Sources");
		resultsGrid.setFrozenColumnCount(1);
		
	}

	@SuppressWarnings({"serial"})
	private void buildMenu() {
		//menu
		mainLayout.addComponent(draftyMenu);
		draftyMenu.setWidth("100%");
		draftyMenu.addStyleName("draftymenu");
		draftyMenu.setHtmlContentAllowed(true);
		Responsive.makeResponsive(draftyMenu);
		
		//draftyLogo MenuItem
		draftyMenu.addItem("Drafty", FontAwesome.UNIVERSITY, new MenuBar.Command() {
			@Override
			public void menuSelected(MenuItem selectedItem) {
				Page.getCurrent().open("/", null);
			}
		});
		
		//aboutMenu
		draftyMenu.addItem("Computer Science Professors from top US and Canada Schools", FontAwesome.GRADUATION_CAP, new MenuBar.Command() {
			
			@Override
			public void menuSelected(MenuItem selectedItem) {
				resetSuggestionMenuItem();
				
				// Create a sub-window and add it to the main window
				Window sub = new Window("About Drafty");
				sub.setWidth("67%");
				VerticalLayout menuModal = new VerticalLayout();
				menuModal.setMargin(true);
				menuModal.setSpacing(true);
				
			    Label label_drafty_title = new Label("<h2 style=\"margin-top: 0px; color:#0095da; margin-bottom: 0px;\"> Drafty </h1>", ContentMode.HTML);
			    Label label_about_title = new Label("<h3 style=\"margin-top: 0px; margin-bottom: 0px;\">Computer Science Professors from Top US and Canadian Schools</h3>", ContentMode.HTML);
			    Label label_hci_title = new Label("<h3 style=\"margin-top: 0px;\">Brown University HCI Project</h3>", ContentMode.HTML);
			    label_about_title.addStyleName("padding-top-none");
			    label_drafty_title.addStyleName("padding-top-none");
			    label_hci_title.addStyleName("padding-top-none");
			    label_drafty_title.setWidth("40px");
			    label_about_title.setWidth("564px");
			    label_hci_title.setWidth("247");
			    
			    Label label_sugg = new Label("<p style=\"margin-top: 0px; padding: 10px; color: #666666; border-radius: 10px; text-align: center; background-color: #f1f1f1;\"<span class=\"v-icon FontAwesome\"></span> <b>Wondering how to make a Suggestion?</b> Double click any cell.</p>", ContentMode.HTML);
			    label_sugg.addStyleName("padding-top-none");
			    label_sugg.setWidth("519px");
			    
			    Label label_about = new Label(

			    	    "<p class=\"projectinfo\" style=\"margin-top: 0px; margin-bottom: 0px;\">"
			    	    + "Drafty turns its web visitors into editors. A visitor to a potentially flawed "
			    	    + "computer science professor dataset may be asked to correct or suggest an entry in the data. "
			    	    + "The key is to capture user interactions such as text highlighting, cursor hovering, clicks, and network "
			    	    + "hostname to infer what topics the visitor has expertise in."
			    	    + "<p style=\"margin-bottom: 0px;\">For example, a visitor from brown.edu who copies entries of graphics professors may be asked "
			    	    + "to correct an entry about graphics professors at Brown. This project is currently being used to "
			    	    + "revise and update data from "
			    	    + "<a style=\"color: blue;\" href=\"http://jeffhuang.com/computer_science_professors.html\">this dataset</a>.</p>"
			    	    + "<p><br><b>Related Paper (HCOMP 2015): </b>"
			    	    + "<a style=\"color: blue;\" href=\"[in press]\"><br>Crowdsourcing from Scratch: A Pragmatic Experiment in Data Collection by "
			    	    + "Novice Requesters</a></p>", ContentMode.HTML);
			    
			    menuModal.addComponents(label_drafty_title,label_about_title, label_hci_title, label_sugg, label_about);
			    menuModal.setComponentAlignment(label_drafty_title, Alignment.MIDDLE_CENTER);
			    menuModal.setComponentAlignment(label_about_title, Alignment.MIDDLE_CENTER);
			    menuModal.setComponentAlignment(label_hci_title, Alignment.MIDDLE_CENTER);
			    menuModal.setComponentAlignment(label_sugg, Alignment.MIDDLE_CENTER);
				sub.setContent(menuModal);
				sub.setModal(true);
				UI.getCurrent().addWindow(sub);
			}
		});
		
		//draftyContact Menu
		draftyMenu.addItem("Contact", FontAwesome.ENVELOPE, new MenuBar.Command() {
			
			@Override
			public void menuSelected(MenuItem selectedItem) {
				resetSuggestionMenuItem();
				
				// Create a sub-window and add it to the main window
				subMail = new Window();
				subMail.setWidth("30%");
				VerticalLayout contactModal = new VerticalLayout();
				contactModal.setMargin(true);
				contactModal.setSpacing(true);
				
			    Label label_about_title = new Label("<h3 style=\"margin-top: 0px;\">Have a question?</h3>", ContentMode.HTML);
			    label_about_title.addStyleName("padding-top-none");
			    
			    fName.setValue("");
			    lName.setValue("");
			    email.setValue("");
			    message.setValue("");
			    submitEmail.setIcon(FontAwesome.ENVELOPE);
			    
			    fName.setWidth("100%");
			    fName.setRequired(true);
			    lName.setWidth("100%");
			    lName.setRequired(true);
			    email.setWidth("100%");
			    email.setRequired(true);
			    message.setWidth("100%");
			    message.setRequired(true);
			    submitEmail.setWidth("100%");
			    contactFieldValueChange(); //checks to see if button needs to be enabled
			    
			    contactModal.addComponents(label_about_title, fName, lName, email, message, submitEmail);
			    
				subMail.setContent(contactModal);
				subMail.setModal(true);
				UI.getCurrent().addWindow(subMail);
				
				//
				submitEmail.addClickListener(new ClickListener() {
					
					private static final long serialVersionUID = -1648581675650203903L;

					@Override
					public void buttonClick(ClickEvent event) {
						
						//first check inputs
						
						MailService mail = new MailService();
						
						//update Profile Info
						try {
							mail.updateProfile(fName.getValue() + " " + lName.getValue(), email.getValue(), idProfile);
						} catch (Exception e) {
							System.out.println("Exception update Profile " + e);
						} finally {
							//only insert comment of Profile is updated
							try {
								mail.insertComment(idProfile, message.getValue());
							} 
							catch (Exception e) {
								System.out.println("Exception Insert Comment " + e);
							}
						}
						
						//close subWindow
						subMail.close();
						Notification.show("Thank you for your comment!  Expect a response in the next few days, or possibly sooner. ;)");
					}
				});
			}
		});
		
		//badgesMenu
		draftyMenu.addItem("Badges", FontAwesome.CERTIFICATE, new MenuBar.Command() {

			@Override
			public void menuSelected(MenuItem selectedItem) {
				updateBadges();
				resetSuggestionMenuItem();
				
				// Create a sub-window and add it to the main window
				Window sub = new Window("Badges");
				VerticalLayout badgesMenunModal = new VerticalLayout();
				badgesMenunModal.setMargin(true);
				//badgesMenunModal.setSpacing(true);
				
				label_badges.setContentMode(ContentMode.HTML);
				
			    badgesMenunModal.addComponents(label_badges, label_badges_info);
				sub.setContent(badgesMenunModal);
				sub.setModal(true);
				UI.getCurrent().addWindow(sub);
			}
		});
		
		//New suggestion button on top right
		suggestionMode = draftyMenu.addItem(icono, new MenuBar.Command() {	
			@Override
			public void menuSelected(MenuItem selectedItem) {
				if (flag_sugg == 1) {
					if (cell_column.equals("FullName")) {
	            		//do nothing
	            		Notification.show("Full Name is not available to make Suggestions");
	            	} else {
	            		new SuggestionComponent(cell_id, cell_full_name, cell_value, cell_column, idProfile);
	            	}	
				} else {
					Notification.show("Please select or double click a cell to make a suggestion.");
				}
			}
		});
		
		suggestionMode.setStyleName("usermenu");
	}
	
	protected void filter(String filter, String column, String blur) {
		try {
			insertFilter(filter, column, blur);
		} catch (SQLException e) {
			System.out.println("Exception  filter(): " + e);
		}
	}
	
	public void insertFilter(String filter, String column, String blur) throws SQLException {  
		try {
	      Context initialContext = new InitialContext();
	      
	      DataSource datasource = (DataSource)initialContext.lookup(DATASOURCE_CONTEXT);
	      if (datasource != null) {
	        Connection conn = datasource.getConnection();
	        String sql = "INSERT INTO Filter (idProfile, idSuggestionType, filter, blur) VALUES (?, (SELECT idSuggestionType FROM SuggestionType WHERE type = ?), ?, ?); ";
	        PreparedStatement stmt = conn.prepareStatement(sql);
	        stmt.setString(1, idProfile);
	        stmt.setString(2, column);
	        stmt.setString(3, filter);
	        stmt.setString(4, blur);
	        try {
		        stmt.executeUpdate();
	        } catch (SQLException e) {
				System.out.println(e.getMessage());
			}
	        stmt.close();
	        conn.close();
	      }
	    }
      catch (Exception ex)
      {
      	System.out.println("Exception" + ex);
      }
	}
	
	public void addFilters() {
		// Create a header row to hold column filters
		HeaderRow filterRow = resultsGrid.appendHeaderRow();

		// Set up a filter for all columns
		for (final Object pid: resultsGrid.getContainerDataSource().getContainerPropertyIds()) {
		    HeaderCell cell = filterRow.getCell(pid);
		    
		    // Have an input field to use for filter
		    final TextField filterField = new TextField();
		    if(pid.equals("JoinYear")) {
			    filterField.setColumns(4);
		    } else if (pid.equals("Rank") || pid.equals("Gender")) {
			    filterField.setColumns(5);
		    } else if (pid.equals("FullName") || pid.equals("University")) {
			    filterField.setColumns(12);
		    } else {
			    filterField.setColumns(21);
		    }
		    filterField.setInputPrompt("Filter");
		    filterField.addStyleName(ValoTheme.TEXTFIELD_TINY);
		    
		    filterField.addFocusListener(new FocusListener() {

				private static final long serialVersionUID = 1004262546138199984L;

				@Override
				public void focus(FocusEvent event) {
					resetSuggestionMenuItem();
				}
		    });
		    
		    filterField.addTextChangeListener(new TextChangeListener() {

			    private static final long serialVersionUID = -448372085933722984L;		
			    String filterText;
		    
				@SuppressWarnings("serial")
				@Override
				public void textChange(TextChangeEvent change) {
					
					// Can't modify filters so need to replace
			    	container.removeContainerFilters(pid);
			    	
			    	//store input
			    	filterText = change.getText();
			    	
			    	//Notification.show("Filter Activate: " + pid + " - " + change.getText());
			    	filter(filterText, pid.toString(), "0");
			    	
			    	
			    	filterField.addBlurListener(new BlurListener() {
						@Override
						public void blur(BlurEvent event) {
							//Notification.show("Filter Activate Blur: " + pid + " - " + filterText);
							filter(filterText, pid.toString(), "1");
						}	
			    	});

					// (Re)create the filter if necessary
			        if (! change.getText().isEmpty())
			            container.addContainerFilter(new SimpleStringFilter(pid,change.getText(), true, false));
				}
			});
		    cell.setComponent(filterField);
		} 
	}
	
	@SuppressWarnings("unchecked")
	private void populateGrid(String lookup) {
		//System.out.println("popgrid 2 start ");
		//clears grid data from grid datasource
		//resultsGrid.getContainerDataSource().removeAllItems();
	    
	    try {
	      Context initialContext = new InitialContext();
	      
	      DataSource datasource = (DataSource)initialContext.lookup(DATASOURCE_CONTEXT);
	      if (datasource != null) {
	        Connection conn = datasource.getConnection();
	        //String sql = "SELECT idPerson, idSuggestionType, suggestion, confidence, name  FROM _view_csv_name";
	        String sql = 
	        		"select o.idPerson AS idPerson,o.idSuggestionType AS idSuggestionType, o.suggestion AS suggestion, o.confidence AS confidence, p.name AS name "
	        		+ "from ((drafty.Suggestion o "
	        		+ "left join drafty.Suggestion b on(((o.idPerson = b.idPerson) and (o.confidence < b.confidence) and (o.idSuggestionType = b.idSuggestionType)))) "
	        		+ "join drafty.Person p on((o.idPerson = p.idPerson))) "
	        		+ "where o.idPerson " + lookup + " AND isnull(b.confidence) order by o.idPerson, o.idSuggestionType "; 
	        		
	        PreparedStatement stmt = conn.prepareStatement(sql);
	        try {
	        	String personId = "";
	        	String typeId = "";
	        	String personIdSt = "1"; //for first record
	        	String typeIdSt = "2"; //always have a university
	        	
	        	String Full_Name = "";
	        	String University = "";
	        	String Bachelors = "";
	        	String Masters = "";
	        	String Doctorate = "";
	        	String PostDoc = "";
	        	String Gender = "";
	        	String Rank = "";
	        	String JoinYear = "";
	        	String Subfield = "";
	        	String PhotoUrl = "";
	        	String Sources = "";
	        	
	        	boolean flag = false;
	        	
				ResultSet rs = stmt.executeQuery();
				while (rs.next()) {
					personId = rs.getString("idPerson");
					typeId = rs.getString("idSuggestionType");
					
					if(personId.equals(personIdSt)) {
						Full_Name = rs.getString("name"); //always the same
						if(typeId.equals("2")) { //
							University = _MainUI.getDataProvider().cleanUniversityName(rs.getString("suggestion"));
						} else if(typeId.equals("3")) { //
							Bachelors = rs.getString("suggestion");
						} else if(typeId.equals("4")) { //
							Masters = rs.getString("suggestion");
						} else if(typeId.equals("5")) { //
							Doctorate = rs.getString("suggestion");
						} else if(typeId.equals("6")) { //
							PostDoc = rs.getString("suggestion");
						} else if(typeId.equals("7")) { //
							JoinYear = rs.getString("suggestion");
						} else if(typeId.equals("8")) { //
							Rank = rs.getString("suggestion");
						} else if(typeId.equals("9")) { //
							Subfield = rs.getString("suggestion");
						} else if(typeId.equals("10")) { //
							Gender = rs.getString("suggestion");
						} else if(typeId.equals("11")) { //
							PhotoUrl = rs.getString("suggestion");
						} else if(typeId.equals("12")) { //
							Sources = rs.getString("suggestion");
						} else {
							//System.out.println(typeId + " " + Full_Name + " " + rs.getString("suggestion"));
						}
					} else {
						if(flag == false) {
							flag = true;
						} else {
							
						}
						
						if(!Full_Name.equals("")) {
							Item newRow = resultsGrid.getContainerDataSource().getItem(resultsGrid.getContainerDataSource().addItem());
							
							newRow.getItemProperty("id").setValue(personIdSt);
							newRow.getItemProperty("FullName").setValue(Full_Name);
						    newRow.getItemProperty("University").setValue(University);
						    newRow.getItemProperty("Bachelors").setValue(Bachelors);
						    newRow.getItemProperty("Masters").setValue(Masters);
						    newRow.getItemProperty("Doctorate").setValue(Doctorate);
						    newRow.getItemProperty("PostDoc").setValue(PostDoc);
						    newRow.getItemProperty("Gender").setValue(Gender);
						    newRow.getItemProperty("Rank").setValue(Rank);
						    newRow.getItemProperty("JoinYear").setValue(JoinYear);
						    newRow.getItemProperty("Subfield").setValue(Subfield);
						    newRow.getItemProperty("PhotoUrl").setValue(PhotoUrl);
						    newRow.getItemProperty("Sources").setValue(Sources);	
						}	
						
						//clears variables
						Full_Name = "";
						University = _MainUI.getDataProvider().cleanUniversityName(rs.getString("suggestion")); //skipped due to logic
			        	Bachelors = "";
			        	Masters = "";
			        	Doctorate = "";
			        	PostDoc = "";
			        	Gender = "";
			        	Rank = "";
			        	JoinYear = "";
			        	Subfield = "";
			        	PhotoUrl = "";
			        	Sources = "";
					}
					
					personIdSt = personId;
					typeIdSt = typeId;
				}
			} catch (SQLException e) {
				System.out.println(e.getMessage());
			}
	        conn.close();
	      }
	    }
        catch (NullPointerException ne)
        {
        	System.out.println("NullPointerException populateGrid(): " + ne);
        } catch (Exception ex) {
        	System.out.println("Exception populateGrid(): " + ex);
		}
	}
	
	private void detectBrowser() {
		if (webBrowser.isChrome()) {
			browser = "Chrome ";
		} else if (webBrowser.isAndroid()) {
			browser = "Android ";
		} else if (webBrowser.isEdge()) {
			browser = "Edge ";
		} else if (webBrowser.isFirefox()) {
			browser = "Firefox ";
		} else if (webBrowser.isIE()) {
			browser = "IE ";
		} else if (webBrowser.isIOS()) {
			browser = "IOS ";
		} else if (webBrowser.isIPad()) {
			browser = "IPad ";
		} else if (webBrowser.isIPhone()) {
			browser = "IPhone ";
		} else if (webBrowser.isOpera()) {
			browser = "Opera ";
		} else if (webBrowser.isSafari()) {
			browser = "Safari ";
		} else if (webBrowser.isWindowsPhone()) {
			browser = "WindwosPhone ";
		}
	}
	
	private void detectCookie() {
		//look at viritin
		//https://github.com/viritin/viritin/blob/830c09c74f722fece45d95adde89354959e5dafa/src/test/java/org/vaadin/viritin/it/BrowserCookieTest.java
		
		//Check for Cookie
		BrowserCookie.detectCookieValue(cookieCheck, new BrowserCookie.Callback() {

            @Override
            public void onValueDetected(String value) {
            	cookieValue = value;
            	
            	System.out.println("cookieCheck " + cookieCheck + " detect cookie:  " + cookieValue + " = " + value);
            	if (cookieValue == null) {
        			System.out.println("cookie value == null : " + cookieValue);
        			
        			//no cookie detected
        			try {
        				newProfile();
        			} catch (SQLException e1) {
        				System.out.println("Profs() newProfile() error: " + e1);
        			} finally {
        				try {
            				newIp();
            			} catch (SQLException e1) {
            				System.out.println("Profs() newIp() error: " + e1);
            			}
        				
            			//sets cookie
            			setCookie();
        			}
        		} else {
        			System.out.println("else, cookie value == " + cookieValue);
        			
        			try {
        				checkProfile();
        			} catch (SQLException e1) {
        				System.out.println("Profs() checkProfile() error: " + e1);
        			} finally {
        				try {
            				checkIpAddress();
            			} catch (SQLException e1) {
            				System.out.println("Profs() checkIpAddress() error: " + e1);
            			}	
        			}
        		}
        		
        		//popgrid -> rest of info; not totally great implementation but it works
        		populateGrid("> 32");	
        		resultsGrid.sort("University");
            }
        });
	}
	
	private void setCookie() {
		//look at viritin
		//https://github.com/viritin/viritin/blob/830c09c74f722fece45d95adde89354959e5dafa/src/test/java/org/vaadin/viritin/it/BrowserCookieTest.java
		
		//set cookie
		BrowserCookie.setCookie(cookieCheck, idProfile);
	}
	
	private String checkProfile() throws SQLException {
		String exists = null;
		
		//still run check to be 100% the id from the cookie is in the system
		
		try {
		      Context initialContext = new InitialContext();
		      
		      DataSource datasource = (DataSource)initialContext.lookup(DATASOURCE_CONTEXT);
		      if (datasource != null) {
		        Connection conn = datasource.getConnection();
		        String sql = "SELECT count(idProfile) as exist, idProfile FROM Profile WHERE idProfile = ? limit 1;";
		        PreparedStatement stmt = conn.prepareStatement(sql);
		        stmt.setString(1, cookieValue);
		        try {
		        	ResultSet rs = stmt.executeQuery();
					while (rs.next()) {
						if(rs.getString("exist").equals("1")) {
							idProfile = rs.getString("idProfile");
							updateProfile();
						}
						exists = rs.getString("exist");
					}
		        } catch (SQLException e) {
					System.out.println(e.getMessage());
				}
		        conn.close();
		      }
		    }
	        catch (Exception ex)
	        {
	        	System.out.println("Exception" + ex);
	        }
			return exists;
	}
	
	private String checkIpAddress() throws SQLException {
		String exists = null;
			
		//update Badges now that we have idProfile
		updateBadges();
		
		try {
	      Context initialContext = new InitialContext();
	      
	      DataSource datasource = (DataSource)initialContext.lookup(DATASOURCE_CONTEXT);
	      if (datasource != null) {
	        Connection conn = datasource.getConnection();
	        String sql = "SELECT count(ip) as exist, idIpAddress FROM IpAddress WHERE idProfile = ? AND ip = ? limit 1;";
	        PreparedStatement stmt = conn.prepareStatement(sql);
	        stmt.setString(1, idProfile);
	        stmt.setString(2, ipAddress);
	        System.out.println("checkIpAddress() cookieValue " + cookieValue);
	        try {
	        	ResultSet rs = stmt.executeQuery();
				while (rs.next()) {
					if(rs.getString("exist").equals("1")) {
						idIpAddress = rs.getString("idIpAddress");
						updateIpAddress();
					} else {
						newIp();
					}
					exists = rs.getString("exist");
				}
	        } catch (SQLException e) {
				System.out.println(e.getMessage());
			}
	        conn.close();
	      }
	    }
        catch (Exception ex)
        {
        	System.out.println("Exception checkIpAddress() " + ex);
        }
		return exists;
	}
	
	private void updateProfile() throws SQLException {
		try {
		      Context initialContext = new InitialContext();
		      
		      DataSource datasource = (DataSource)initialContext.lookup(DATASOURCE_CONTEXT);
		      if (datasource != null) {
		        Connection conn = datasource.getConnection();
		        String sql = "UPDATE Profile SET date_updated = ?, logins = logins + 1 WHERE idProfile = ? ";
		        PreparedStatement stmt = conn.prepareStatement(sql);
		        stmt.setString(1, new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss").format(new Date()));
		        stmt.setString(2, idProfile);
		        try {
			        stmt.executeUpdate();
		        } catch (SQLException e) {
					System.out.println(e.getMessage());
				}
		        conn.close();
		      }
		    }
	        catch (Exception ex)
	        {
	        	System.out.println("Exception Profs.java updateProfile() " + ex);
	        }
	    }
	
	private void updateIpAddress() throws SQLException {
		try {
		      Context initialContext = new InitialContext();
		      
		      DataSource datasource = (DataSource)initialContext.lookup(DATASOURCE_CONTEXT);
		      if (datasource != null) {
		        Connection conn = datasource.getConnection();
		        String sql = "UPDATE IpAddress SET date_updated = ?, logins = logins + 1, browser = ?, locale = ? WHERE idIpAddress = ?; ";
		        PreparedStatement stmt = conn.prepareStatement(sql);
		        stmt.setString(1, new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss").format(new Date()));
		        stmt.setString(2, browser + browserNumber);
		        stmt.setString(3, webBrowser.getLocale().toString());
		        stmt.setString(4, idIpAddress);
		        
		        System.out.println("updateIpAddress() cookieValue " + cookieValue);
		        try {
			        stmt.executeUpdate();
		        } catch (SQLException e) {
					System.out.println(e.getMessage());
				}
		        conn.close();
		      }
		    }
	        catch (Exception ex)
	        {
	        	System.out.println("Exception updateIpAddress() " + ex);
	        }
	    }
	
	private void newProfile() throws SQLException {
		try {
	      Context initialContext = new InitialContext();
	      
	      DataSource datasource = (DataSource)initialContext.lookup(DATASOURCE_CONTEXT);
	      if (datasource != null) {
	        Connection conn = datasource.getConnection();
	        String sql = "INSERT INTO Profile (date_created, date_updated, logins) VALUES (?, ?, 1); ";
	        PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
	        stmt.setString(1, new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss").format(new Date()));
	        stmt.setString(2, new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss").format(new Date()));
	        try {
		        int affectedRows = stmt.executeUpdate();
		        
		        if (affectedRows == 0) {
		            throw new SQLException("Creating failed, no rows affected.");
		        }
		        try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
		            if (generatedKeys.next()) {
		        		idProfile = generatedKeys.getString(1);
		        		System.out.println("newProfile() idProfile " + idProfile);
		            }
		            else {
		                throw new SQLException("Creating failed, no ID obtained.");
		            }
		        }
	        } catch (SQLException e) {
				System.out.println(e.getMessage());
			}
	        conn.close();
	      }
	    }
        catch (Exception ex)
        {
        	System.out.println("Exception" + ex);
        }
	}
	
	private void newIp() throws SQLException {
		try {
	      Context initialContext = new InitialContext();
	      
	      DataSource datasource = (DataSource)initialContext.lookup(DATASOURCE_CONTEXT);
	      if (datasource != null) {
	        Connection conn = datasource.getConnection();
	        String sql = "INSERT INTO IpAddress (idProfile, ip, browser, locale, date_created, date_updated, logins) VALUES (?, ?, ?, ?, ?, ?, 1); ";
	        PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
	        //System.out.println("newIp() idProfile " + idProfile);
	        stmt.setString(1, idProfile);
	        stmt.setString(2, ipAddress);
	        stmt.setString(3, webBrowser.getLocale().toString());
	        stmt.setString(4, browser + browserNumber);
	        stmt.setString(5, new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss").format(new Date()));
	        stmt.setString(6, new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss").format(new Date()));
	        try {
		        int affectedRows = stmt.executeUpdate();
		        
		        if (affectedRows == 0) {
		            throw new SQLException("Creating failed, no rows affected.");
		        }
		        try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
		            if (generatedKeys.next()) {
		        		idIpAddress = generatedKeys.getString(1);
		            }
		            else {
		                throw new SQLException("Creating failed, no ID obtained.");
		            }
		        }
	        } catch (SQLException e) {
				System.out.println("newIp() error: " + e.getMessage());
			}
	        conn.close();
	      }
	    }
        catch (Exception ex)
        {
        	System.out.println("Exception" + ex);
        }
	}
	
	private void resetSuggestionMenuItem() {
		flag_sugg = 0;
    	suggestionMode.setText(icono);
	}
	
	@Override
	public void enter(ViewChangeEvent event) {
		
	}	
	
	public void afterViewChange(ViewChangeListener event) {
		//System.out.println("View After");
    }
}
