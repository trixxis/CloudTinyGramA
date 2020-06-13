package foo;

import com.google.appengine.api.datastore.Key;

public class PostMessage {
	public String owner;
	public String body;
	public String url;
	public Key    key;
	
	public PostMessage() {}
}
