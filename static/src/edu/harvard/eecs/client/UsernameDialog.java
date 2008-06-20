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
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.DialogBox;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

public class UsernameDialog extends DialogBox {

	   TextBox namefield = null;
	    public UsernameDialog(Latiki latiki) {
	      setText("Username");
	  
	      // fixme: there MUST be a better way to do this
	      final Latiki[] latikis = {latiki};
	      Button closeButton = new Button("OK", new ClickListener() {
					public void onClick(Widget sender) {
					    String username = namefield.getText().replaceAll(
								"[^a-zA-Z0-9._-]", ".");
						if (username.equals("")) {
						        username = "Anonymous";
						}

						latikis[0].start(username);
						hide();
					}
				});

	      VerticalPanel panel = new VerticalPanel();
	      panel.add(new Label("Please enter a username for this session"));
	      namefield = new TextBox();
	      panel.add(namefield);
	      panel.add(closeButton);


	      setWidget(panel);
	    }

	


}
