package edu.harvard.eecs.client;

/*

This file is part of Latiki, a web-based editing and typesetting program
Copyright (C) 2008 Joseph Barillari & Tim Credo

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>.


*/

import java.util.HashMap;

import java.util.Iterator;
import com.google.gwt.json.client.JSONArray;
import com.google.gwt.json.client.JSONException;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONParser;
import com.google.gwt.json.client.JSONString;
import com.google.gwt.json.client.JSONValue;
import com.google.gwt.http.client.RequestBuilder;
import com.google.gwt.http.client.RequestException;
import com.google.gwt.http.client.RequestCallback;
import com.google.gwt.http.client.Request;
import com.google.gwt.http.client.Response;
import com.google.gwt.user.client.ui.Tree;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.DisclosurePanel;
import com.google.gwt.user.client.ui.DialogBox;
import com.google.gwt.user.client.ui.TreeItem;
import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.user.client.ui.TextArea;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.TabPanel;
import com.google.gwt.user.client.ui.TabListener;
import com.google.gwt.user.client.ui.SourcesTabEvents;

/**
 * FIXME: note that this requires a hacked version of cherrypy (one-line hack to
 * assume form is urlencoded even if headers say otherwise) Entry point classes
 * define <code>onModuleLoad()</code>.
 */
public class Latiki implements EntryPoint {
	public static final String FILELIST_URL = "/repo/filelist";

	public static final String SAVE_URL = "/repo/save";

	public static final String LOADTEXT_URL = "/repo/loadText";

	public static final String MAKEREPOSITORY_URL = "/repo/makeRepository";

	public static final String COMPARE_URL = "/repo/compareRevisions";

	public static final String REVISIONLIST_URL = "/repo/listRevisions";

	public static final String GETREVISION_URL = "/repo/getRevision";

	public static final String TYPE = "type";

	public static final String PATH = "path";

	public static final String NAME = "name";

	public static final String FILE = "file";

	public static final String DIR = "dir";

	public static final String EDITABLE = "editable";

	public static final String TRUE = "true";

	DisclosurePanel uploadDisc = null;

	TextBox titleBox = null;

	TextBox authorsBox = null;

	TextBox dateBox = null;

	ListBox styleBox = null;

	// TextArea logArea = null;
	DiffPanel diffpanel = null;

	VerticalPanel diffHolder = null;

	ListBox compareBox = null;

	TextArea abstractArea = null;

	VerticalPanel latexpanel = null;

	VerticalPanel revisionPanel = null;

	TabPanel contentTabs = null;

	TabPanel navTabs = null;

	TextArea textArea = null;

	Tree fileTree = null;

	String prevText = null;
	
	String repoid = null;

	String currentPath = null;

	String currentRevision = null;

	Label fileLabel = null;

	String userName = "";

	// We need to use this instead of URI.encode because the latter
	// doesn't escape ';' and so forth, which breaks the cherrypy cgi
	// parser. sigh.
	public native String encodeURIComponent(String arg) /*-{
	 return encodeURIComponent(arg);
	 }-*/;

	public native void reloadFrame(String loc) /*-{
	 $wnd.frames['pdframe'].document.location = loc;                  
	 }-*/;

	public native boolean confirm(String msg) /*-{
	 return confirm(msg);
	 }-*/;

	// fixme: hack!
	public native String getRepoIdInner() /*-{
	 var pair = $doc.location.search.replace("?","").split(/=/);
	 if (pair.length < 2) {
	 alert("ERROR! Repoid is null! querystring = " + $doc.location.search);
	 return null;
	 }
	 return pair[1].toString();
	 }-*/;

	public native void defineSetBodyMethod() /*-{
	 var savethis = this;
	 $wnd.setBody = function(st) {
	 return savethis.@edu.harvard.eecs.client.Latiki::setBody(Ljava/lang/String;)(st);
	 }                  
	 }-*/;

	public native void defineGetAllMethod() /*-{
	 var savethis = this;
	 $wnd.getAll = function() {
	 return [	savethis.@edu.harvard.eecs.client.Latiki::getBody()(),
	 savethis.@edu.harvard.eecs.client.Latiki::getTitle()(),
	 savethis.@edu.harvard.eecs.client.Latiki::getAuthors()(),
	 savethis.@edu.harvard.eecs.client.Latiki::getStyle()(),
	 savethis.@edu.harvard.eecs.client.Latiki::getAbstract()(),
	 savethis.@edu.harvard.eecs.client.Latiki::getDate()()];
	 }                  
	 }-*/;

	public native void alert(String msg) /*-{
	 alert(msg);
	 
	 }-*/;
	
	public String getRepoId() {
		// to prevent the user from changing it without a reload and confusing us.
		// (can that happen? will we see an edited but un-fetched url?)
		if (repoid == null) {
			repoid = getRepoIdInner();
		}
		return repoid;
	}

	public void setBody(String st) {
		if (textArea != null) {
			textArea.setText(st);
		}
		prevText = st;
	}

	public String getBody() {
		if ((textArea != null) && (textArea.isVisible() == true)) {
			return textArea.getText();
		}
		if (diffHolder != null) {
			return diffpanel.getOutput();
		}

		return "";
	}

	public String getTitle() {
		if (titleBox != null) {
			return titleBox.getText();
		}
		return "";
	}

	public String getAuthors() {
		if (authorsBox != null) {
			return authorsBox.getText();
		}
		return "";
	}

	public String getDate() {
		if (dateBox != null) {
			return dateBox.getText();
		}
		return "";
	}

	public String getStyle() {
		return styleBox.getValue(styleBox.getSelectedIndex());
	}

	public String getAbstract() {
		if (abstractArea != null) {
			return abstractArea.getText();
		}
		return "";
	}

	public JSONObject packData() {
		JSONObject dict = new JSONObject();
		dict.put("body", new JSONString(getBody()));
		dict.put("title", new JSONString(getTitle()));
		dict.put("authors", new JSONString(getAuthors()));
		dict.put("date", new JSONString(getDate()));
		dict.put("style", new JSONString(getStyle()));
		dict.put("abstract", new JSONString(getAbstract()));
		return dict;
	}

	/**
	 * A simple dialog box that displays a message, a Frame, and a close button.
	 */
	// private static class MyDialog extends DialogBox implements ClickListener
	// {
	// public MyDialog() {
	// setText("Confirm load");
	// Button cancelButton = new Button("Cancel", this);
	// Button continueButton = new Button("Continue", this);
	// HTML msg = new HTML(
	// "<center>You have unsaved changes which will be lost if you continue.
	// Continue?</center>",
	// true);
	// HorizontalPanel hp = new HorizontalPanel();
	// hp.add(cancelButton);
	// hp.add(continueButton);
	// VerticalPanel vp = new VerticalPanel();
	// vp.add(msg);
	// vp.add(hp);
	// setWidget(vp);
	// }
	// public void onClick(Widget sender) {
	// hide();
	// }
	// }
	/**
	 * Class for handling the response text associated with a request for a JSON
	 * object. swiped from GWT's JSON.java. (c) 2006 google
	 * 
	 */
	private class JSONHandler implements RequestCallback {
	    public void onError(Request request, java.lang.Throwable exception) {
		alert("unexpected error in JSONHandler: " + request + " exception " + exception);
	    }
	    public void onResponseReceived(Request request, Response response) {
		String responseText = response.getText();
			try {
			    JSONValue jsonValue = JSONParser.parse(responseText);
			    displayJSONObject(jsonValue);
			} catch (JSONException e) {
			    displayError(responseText);
			}
	    }
	}

	private class RevisionListAndCurrentRevisionHandler implements
			RequestCallback {
	    public void onError(Request request, java.lang.Throwable exception) {
		alert("unexpected error in RevisionListAndCurrentRevisionHandler: " + request 
		      + " exception " + exception);
	    }

	    public void onResponseReceived(Request request, Response response) {
		String responseText = response.getText();
		try {
		    JSONValue jsonValue = JSONParser.parse(responseText);
		    JSONArray jsonArray;
		    if ((jsonArray = jsonValue.isArray()) == null) {
					alert("v parse error");
					return;
		    }
		    displayRevisionList(jsonArray);
		    if (jsonArray.size() > 0) {
			// if there are any existing revisions, try loading oneq
			loadRevision(null);
		    }

		} catch (JSONException e) {
		    alert("revision list parse failed");
		}
	    }
	}

//	private class LoadTextHandler implements ResponseTextHandler {
//		public void onCompletion(String responseText) {
//			prevText = responseText;
//			setBody(responseText);
//			textArea.setEnabled(true);
//		}
//	}

	private class CompareHandler implements RequestCallback {
	    public void onError(Request request, java.lang.Throwable exception) {
		alert("unexpected error in CompareHandler: " + request + " exception " + exception);
	    }

	    public void onResponseReceived(Request request, Response response) {
		String responseText = response.getText();

			if (responseText.equals("fail")) {
				alert("compare failed.");
			} else {

				textArea.setVisible(false);
				uploadDisc.setVisible(false);
				contentTabs.selectTab(1);
				if (diffHolder != null) {
					latexpanel.remove(diffHolder);
				}
				diffHolder = new VerticalPanel();
				diffHolder.addStyleName("whitebkgnd");
				diffpanel = new DiffPanel(responseText);
				diffHolder.add(new Label("Comparing versions:"));
				diffHolder.add(diffpanel);
				Button cancel = new Button("Cancel comparison");
				cancel.addClickListener(new ClickListener() {
					public void onClick(Widget sender) {
						closeDiff();
					}
				});
				diffHolder.add(cancel);
				latexpanel.add(diffHolder);

			}
		}
	}

	private void closeDiff() {
		textArea.setVisible(true);
		uploadDisc.setVisible(true);
		if (diffHolder != null) {
			latexpanel.remove(diffHolder);
			diffHolder = null;
		}
	}

	private class SaveHandler implements RequestCallback {
	    public void onError(Request request, java.lang.Throwable exception) {
		alert("unexpected error in SaveHandler: " + request 
		      + " exception " + exception);
	    }

	    public void onResponseReceived(Request httpRequest, Response httpResponse) {
		String responseText = httpResponse.getText();
			JSONObject response;
			JSONObject result;
			if ((response = JSONParser.parse(responseText).isObject()) == null) {
				alert("Save error: invalid JSON:" + responseText);
				return;
			}
			if (response.get("error").isString() != null) {
				alert("Save error:"
						+ response.get("error").isString().stringValue());
				return;
			}
			if ((result = response.get("result").isObject()) == null) {
				alert("Save error: invalid JSON result:" + responseText);
				return;
			}
			// return state can be "new", "nochange", or "conflict"
			String state = result.get("state").isString().stringValue();
			if (state.equals("new")) {
				currentRevision = result.get("newrevision").isString()
						.stringValue();
				addRevision(currentRevision, result.get("user").isString()
						.stringValue(), result.get("date").isString()
						.stringValue());
				updateRevisionHighlight();
				setBody(getBody()); // update prevText; update textarea with current body
				// if we were in diff mode, close it. also update the textarea
				// with the current body
				if (diffHolder != null) {
					closeDiff();
				}
			} else if (state.equals("nochange")) {
				alert("No changes since last save.");
				setBody(getBody()); 
				if (diffHolder != null) {
					closeDiff();
				}
			} else if (state.equals("conflict")) {
				alert("Conflict! Someone has edited a newer revision. we don't handle this (yet).");
			} else {
				alert("save: invalid state:" + state);
			}
		}
	}
    
    private boolean makeHTTPPOST(String url, String data, RequestCallback callback) {
	try {
	    RequestBuilder rb = new RequestBuilder(RequestBuilder.POST, url);
	    rb.setHeader("Content-Type", "application/x-www-form-urlencoded");
	    rb.sendRequest(data, callback);
	    return true;
	}
	catch (RequestException e) {
	    alert("Request failed with exception " + e); // fixme: return an error code!
	    return false;
	}
    }


	private void loadRevision(String number) {
		String params = "repoid=" + encodeURIComponent(getRepoId());
		if (number != null) {
			// if revision is blank, it defaults to the latest
			params += "&revision=" + number;
		}
		if (number != null && currentRevision == number) {
			alert("Revision " + number + " already loaded.");
			return;
		}
		if (!confirmDataLoss()) {
			return;
		}
		makeHTTPPOST(GETREVISION_URL, params, new GetRevisionHandler());

	}

	private boolean confirmDataLoss() {
		if (prevText != null && (!getBody().equals(prevText))) {
			// FIXME! check everything, not just the body!
			// (avoiding a tedious check of all get* methods here
			// because they need to be replaced for the configurable-metadata
			// anyway
			return confirm("Unsaved changes will be lost! Continue anyway?");
		}
		return true;
	}
	
	private void addRevision(String number, String user, String date) {
		Label label = new Label("Revision " + number + " (" + user + ")");
		label.setTitle(number); // this is a horrid hack. keep an index
		// elsewhere.
		label.addClickListener(new ClickListener() {
			public void onClick(Widget sender) {
				// why can't you close over local vars in java?
				String lrev = ((Label) sender).getTitle();

				if (!(lrev.equals(currentRevision))
						|| textArea.isVisible() == false) {
					closeDiff();
					loadRevision(lrev);
				} else {
					alert("Revision " + lrev + " already loaded.");
				}

			}
		});
		revisionPanel.add(label);
		compareBox.addItem(number);

	}

	private class GetRevisionHandler implements RequestCallback {
	    public void onError(Request request, java.lang.Throwable exception) {
		alert("unexpected error in GetRevisionHandler: " + request + " exception " + exception);
	    }
	    public void onResponseReceived(Request httpRequest, Response httpResponse) {
		String responseText = httpResponse.getText();

			JSONObject response;
			if ((response = JSONParser.parse(responseText).isObject()) == null) {
				alert("could not parse revision: " + responseText);
			}

			setBody(response.get("body").isString().stringValue());
			titleBox.setText(response.get("title").isString().stringValue());
			authorsBox
					.setText(response.get("authors").isString().stringValue());
			dateBox.setText(response.get("date").isString().stringValue());
			abstractArea.setText(response.get("abstract").isString()
					.stringValue());
			for (int i = 0; i < styleBox.getItemCount(); i++) {
				if (styleBox.getValue(i).equals(
						response.get("date").isString().stringValue())) {
					styleBox.setSelectedIndex(i);
				}
			}
			// fixme: do something on failure to match!
			currentRevision = response.get("revnum").isString().stringValue();
			updateRevisionHighlight();
		}
	}

	private void updateRevisionHighlight() {
		Iterator iter = revisionPanel.iterator();
		while (iter.hasNext()) {
			Label label = (Label) iter.next();
			label.removeStyleName("highlighted");
			if (label.getTitle().equals(currentRevision)) {
				label.addStyleName("highlighted");
			}
		}
	}

	private void displayRevisionList(JSONArray jsonArray) {
		JSONArray itemArray;

		for (int i = 0; i < jsonArray.size(); ++i) {
			if ((itemArray = jsonArray.get(i).isArray()) == null) {
				alert("revision inner parse error");
				return;
			}
			String number = itemArray.get(0).isString().stringValue();
			String date = itemArray.get(1).isString().stringValue();
			String user = itemArray.get(2).isString().stringValue();

			addRevision(number, user, date);

		}
	}

	/*
	 * Add the object presented by the JSONValue as a children to the requested
	 * TreeItem. method swiped from GWT's JSON.java. (c) 2006 google
	 */
	private void addChildren(TreeItem treeItem, JSONValue jsonValue) {
		JSONArray jsonArray;
		JSONArray itemArray;
		// the returned structures are lists of the form [dir|file, name, path,
		// children]
		// children is empty for files. (duh)
		if ((jsonArray = jsonValue.isArray()) == null) {
			alert("parse error");
			return;
		}
		for (int i = 0; i < jsonArray.size(); ++i) {
			if ((itemArray = jsonArray.get(i).isArray()) == null) {
				alert("inner parse error");
				return;
			}
			String type = itemArray.get(0).isString().stringValue();
			String name = itemArray.get(1).isString().stringValue();
			String path = itemArray.get(2).isString().stringValue();
			HashMap hm = new HashMap();
			hm.put(TYPE, type);
			hm.put(NAME, name);
			hm.put(PATH, path);
			TreeItem newItem = treeItem.addItem(name
					+ (type.equals("dir") ? "/" : ""));
			newItem.setUserObject(hm);
			JSONArray children = itemArray.get(3).isArray();
			if (children != null && children.size() > 0) {
				addChildren(newItem, children);
			}
		}
	}

	/*
	 * Causes the text of child elements to wrap. method swiped from GWT's
	 * JSON.java. (c) 2006 google
	 */

	private String getChildText(String text) {
		return "<span style='white-space:normal'>" + text + "</span>";
	}

	/*
	 * Update the treeview of a JSON object. method swiped from GWT's JSON.java.
	 * (c) 2006 google
	 */
	private void displayJSONObject(JSONValue jsonValue) {
		fileTree.removeItems();
		fileTree.setVisible(true);
		TreeItem treeItem = fileTree.addItem("Repository");
		addChildren(treeItem, jsonValue);
		// treeItem.setStyleName("JSON-JSONResponseObject");
		treeItem.setState(true);
	}

	/*
	 * fail to display json method swiped from GWT's JSON.java. (c) 2006 google
	 */

	private void displayError(String responseText) {
		fileTree.removeItems();
		fileTree.setVisible(true);
		TreeItem treeItem = fileTree.addItem("Failed to parse JSON response");
		treeItem.addItem(responseText);
		treeItem.setStyleName("JSON-JSONResponseObject");
		treeItem.setState(true);
	}

//	/*
//	 * load new content into the textarea
//	 * 
//	 */
//	public void textAreaLoad(String path, String revision) {
//		if (path.equals(currentPath) && revision.equals(currentRevision)) {
//			return;
//			// fixme: fails when currentRevision is the numebr of the current
//			// revision and revision is "current" or vice-versa...must figure
//			// out which one is current...
//		}
//
//		if (prevText != null && (!getBody().equals(prevText))) {
//			if (!confirm("Unsaved changes will be lost. Continue?")) {
//				return;
//			}
//		}
//		fileLabel.setText("Path: " + path);
//		currentPath = path;
//		textArea.setEnabled(false);
//		textArea.setText("");
//		String params = "repoid=" + encodeURIComponent(getRepoId()) + "&path="
//				+ encodeURIComponent(path) + "&revision=" + revision;
//		HTTPRequest.asyncPost(LOADTEXT_URL, params, new LoadTextHandler());
//		// fixme: handle errors somehow.
//
//	}

	public void loadRevisionListAndCurrentRevision() {
		revisionPanel.clear();
		// fixme: should be POST!
		makeHTTPPOST(REVISIONLIST_URL, "repoid="
				+ encodeURIComponent(getRepoId()),
				new RevisionListAndCurrentRevisionHandler());
	}

	// load files into the tree
	public void loadFileTree() {
		fileTree.clear();
		// fixme: should be POST!
		makeHTTPPOST(FILELIST_URL, "repoid="
				+ encodeURIComponent(getRepoId()),
				new JSONHandler());
	}

	/**
	 * This is the entry point method.
	 */
	public void onModuleLoad() {
		if (getRepoId() == null) {
			RootPanel
					.get()
					.add(
							new Label(
									"You must specify a repository id. Use a query string with the form ?repoid=your_repo_id and reload the page to retry."));
			return;
		} else {

			String params = "repoid="
			    + encodeURIComponent(getRepoId());
			//alert("calling makerepository with params: " + params);
			makeHTTPPOST(MAKEREPOSITORY_URL, params,
					new StartupMakeRepositoryHandler(this));
		}

	}

	private class StartupMakeRepositoryHandler implements RequestCallback {
		Latiki latiki_;

		public StartupMakeRepositoryHandler(Latiki latiki) {
			latiki_ = latiki;
		}


	    public void onError(Request request, java.lang.Throwable exception) {
		alert("unexpected error in StartupMakeRepositoryHandler: " + request + " exception " + exception);
	    }
	    public void onResponseReceived(Request request, Response response) {
		String responseText = response.getText();

			if (responseText.equals("new") || responseText.equals("exists")) {
				DialogBox dlg = new UsernameDialog(latiki_);
				dlg.center();
			} else {
				RootPanel
						.get()
						.add(
								new Label(
										"invalid repository id '"
												+ getRepoId()
												+ "' or backend failure:"
												+ responseText
												+ ". Reload to retry; change the repoid if necessary."));
				return;
			}
		}
	}

	public void start(String username) {
		userName = username;
		contentTabs = new TabPanel();
		latexpanel = new VerticalPanel();
		latexpanel.setSpacing(4);
		textArea = new TextArea();
		textArea.setWidth("40em");
		textArea.setVisibleLines(20);
		latexpanel.add(textArea);

		uploadDisc = new DisclosurePanel("Upload");
		HorizontalPanel uploadPanel = new HorizontalPanel();
		uploadDisc.add(uploadPanel);
		Element uploadIframe = DOM.createIFrame();
		DOM.setElementProperty(uploadIframe, "id", "ulframe");
		DOM.setElementProperty(uploadIframe, "name", "ulframe");
		DOM.setElementProperty(uploadIframe, "width", "600");
		DOM.setElementProperty(uploadIframe, "height", "100");
		DOM.setElementProperty(uploadIframe, "src", "/upload");
		DOM.appendChild(uploadPanel.getElement(), uploadIframe);

		latexpanel.add(uploadDisc);

		titleBox = new TextBox();
		titleBox.setWidth("40em");
		authorsBox = new TextBox();
		authorsBox.setWidth("40em");
		dateBox = new TextBox();
		dateBox.setWidth("40em");
		styleBox = new ListBox();
		styleBox.addItem("generic");
		styleBox.addItem("IEEE");
		styleBox.addItem("Elsevier1");
		styleBox.addItem("Elsevier2");
		styleBox.addItem("AMS");
		abstractArea = new TextArea();
		abstractArea.setWidth("40em");
		abstractArea.setVisibleLines(10);

		Grid grid = new Grid(6, 2);
		grid.setText(0, 0, "Title");
		grid.setWidget(0, 1, titleBox);

		grid.setText(1, 0, "Author");
		grid.setWidget(1, 1, authorsBox);

		grid.setText(2, 0, "Date");
		grid.setWidget(2, 1, dateBox);

		grid.setText(3, 0, "Style");
		grid.setWidget(3, 1, styleBox);

		grid.setText(4, 0, "Abstract");
		grid.setWidget(4, 1, abstractArea);

		fileLabel = new Label("-nothing loaded-");

		HorizontalPanel pdfpanel = new HorizontalPanel();

		Element iframe = DOM.createIFrame();
		DOM.setElementProperty(iframe, "id", "pdframe");
		DOM.setElementProperty(iframe, "name", "pdframe");
		DOM.setElementProperty(iframe, "height", "540");
		DOM.setElementProperty(iframe, "width", "640");
		DOM.appendChild(pdfpanel.getElement(), iframe);

		defineGetAllMethod();
		defineSetBodyMethod();

		contentTabs.addTabListener(new TabListener() {
			public void onTabSelected(SourcesTabEvents sender, int tabIndex) {
				return; // noop
			}

			public boolean onBeforeTabSelected(SourcesTabEvents sender,
					int tabIndex) {
				if (tabIndex == 2) { // FIXME! don't hardcode this
					// pdf panel
					if (true || !textArea.getText().equals(prevText)) { // fixme!
						// why
						// can't
						// we
						// switch
						// back?
						prevText = textArea.getText();
						// RootPanel.get().add(new Label("text differs!
						// a='"+prevText+"' '"+ textArea.getText()+ "'"));
						reloadFrame(GWT.getModuleBaseURL() + "submit.html");
					}
				}

				return true;
			}
		});

		contentTabs.add(grid, "Properties");
		contentTabs.add(latexpanel, "Body");
		contentTabs.add(pdfpanel, "PDF");
		contentTabs.setWidth("100%");
		contentTabs.selectTab(0);

		VerticalPanel leftPanel = new VerticalPanel();
		// revisions.add(new Label("Revision history repoid=" + getRepoId() ));
		revisionPanel = new VerticalPanel();
		leftPanel.add(revisionPanel);

		compareBox = new ListBox();
		HorizontalPanel compareToPanel = new HorizontalPanel();
		Button compareButton = new Button("Go", new ClickListener() {

			public void onClick(Widget sender) {
				// fixme: make it more obvious that we're comparing the last saved version
				// not the onscreen one. (or add a "use saved", "save first", cancel) dialog.
				if (!confirmDataLoss()) {
					return;
				}
				
				String crev = compareBox
						.getValue(compareBox.getSelectedIndex());
				// fixme! check if current text modified!
				if (crev.equals(currentRevision)) {
					alert("You can't compare a revision to itself.");
					return;
				}
				String params = "repoid=" + encodeURIComponent(getRepoId())
						+ "&fromrev=" + crev + "&torev=" + currentRevision;
				makeHTTPPOST(COMPARE_URL, params, new CompareHandler());
			}
		});
		compareButton.setWidth("30px");

		compareToPanel.add(new Label("Compare to:"));
		compareToPanel.add(compareBox);
		compareToPanel.add(compareButton);
		leftPanel.add(compareToPanel);

		Button saveButton = new Button("Save");

		saveButton.addClickListener(new ClickListener() {
			public void onClick(Widget sender) {
				// gather all text, send it to the server.
				// if anything has changed, create a new revision
				// if nothing has changed, tell the user so.

				// fixme: if something has changed behind our back
				// (e.g., revision we started with is older than the
				// latest), alert the user (c.f. wikipedia).

				JSONObject jsonData = packData();
				String params = "repoid=" + encodeURIComponent(getRepoId())
						+ "&data=" + encodeURIComponent(jsonData.toString())
						+ "&currentRevision=" + currentRevision + "&user="
						+ userName; // fixme: make settable
				// logArea.setText(params);
				makeHTTPPOST(SAVE_URL, params, new SaveHandler());
			}
		});
		leftPanel.add(saveButton);

		// revisionList.addTreeListener( new TreeListener() {
		// public void onTreeItemSelected(TreeItem item) {
		// String vernum = (String) item.getUserObject();
		// if (vernum != null) {
		// textAreaLoad(currentPath, vernum);
		// }
		// }
		// public void onTreeItemStateChanged(TreeItem item) {
		// // do nothing
		// }
		// }
		// );

		// VerticalPanel filepanel = new VerticalPanel();
		// fileTree = new Tree();
		// fileTree.addTreeListener( new TreeListener() {
		// public void onTreeItemSelected(TreeItem item) {
		// HashMap hm = (HashMap) item.getUserObject();
		// if (hm != null) {
		// if ( ((String) hm.get(TYPE)).equals(FILE)) {
		// textAreaLoad((String) hm.get(PATH), "current");
		// loadRevisionList((String) hm.get(PATH));
		// }
		// }
		// }
		// public void onTreeItemStateChanged(TreeItem item) {
		// // do nothing
		// }
		// }
		// );
		// filepanel.add(fileTree);
		// loadFileTree();

		navTabs = new TabPanel();
		// navTabs.add(filepanel, "Files");
		navTabs.add(leftPanel, "Revisions");

		loadRevisionListAndCurrentRevision();

		navTabs.selectTab(0);

		HorizontalPanel base = new HorizontalPanel();
		base.add(navTabs);
		base.add(contentTabs);

		// logArea = new TextArea();
		// logArea.setWidth("40em");
		// logArea.setVisibleLines(20);
		RootPanel.get().add(base);
		// RootPanel.get().add(logArea);
		// insert the username dialog

	}
}
