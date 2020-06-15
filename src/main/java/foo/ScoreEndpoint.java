package foo;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import com.google.api.server.spi.auth.common.User;
import com.google.api.server.spi.config.Api;
import com.google.api.server.spi.config.ApiMethod;
import com.google.api.server.spi.config.ApiMethod.HttpMethod;
import com.google.api.server.spi.config.ApiNamespace;
import com.google.api.server.spi.config.Named;
import com.google.api.server.spi.config.Nullable;
import com.google.api.server.spi.response.CollectionResponse;
import com.google.api.server.spi.response.UnauthorizedException;
import com.google.api.server.spi.auth.EspAuthenticator;

import com.google.appengine.api.datastore.Cursor;
import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.FetchOptions;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.datastore.PreparedQuery;
import com.google.appengine.api.datastore.PropertyProjection;
import com.google.appengine.api.datastore.PreparedQuery.TooManyResultsException;
import com.google.appengine.api.datastore.Query.CompositeFilter;
import com.google.appengine.api.datastore.Query.CompositeFilterOperator;
import com.google.appengine.api.datastore.Query.Filter;
import com.google.appengine.api.datastore.Query.FilterOperator;
import com.google.appengine.api.datastore.Query.FilterPredicate;
import com.google.appengine.api.datastore.Query.SortDirection;
import com.google.appengine.api.datastore.QueryResultList;
import com.google.appengine.api.datastore.Transaction;

@Api(name = "myApi",
     version = "v1",
     audiences = "689297071615-irgk1dtiu0ts6bntktqmshsc7u44610o.apps.googleusercontent.com",
  	 clientIds = "689297071615-irgk1dtiu0ts6bntktqmshsc7u44610o.apps.googleusercontent.com",
     namespace =
     @ApiNamespace(
		   ownerDomain = "webandcloud-273109.appspot.com",
		   ownerName = "webandcloud-273109.appspot.com",
		   packagePath = "")
     )

public class ScoreEndpoint {

	Random r = new Random();

	@ApiMethod(name = "getRandom", httpMethod = HttpMethod.GET)
	public RandomResult random() {
		return new RandomResult(r.nextInt(6) + 1);
	}

	@ApiMethod(name = "scores", httpMethod = HttpMethod.GET)
	public List<Entity> scores() {
		Query q = new Query("Score").addSort("score", SortDirection.DESCENDING);

		DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
		PreparedQuery pq = datastore.prepare(q);
		List<Entity> result = pq.asList(FetchOptions.Builder.withLimit(100));
		return result;
	}

	@ApiMethod(name = "topscores", httpMethod = HttpMethod.GET)
	public List<Entity> topscores() {
		Query q = new Query("Score").addSort("score", SortDirection.DESCENDING);

		DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
		PreparedQuery pq = datastore.prepare(q);
		List<Entity> result = pq.asList(FetchOptions.Builder.withLimit(10));
		return result;
	}

	@ApiMethod(name = "myscores", httpMethod = HttpMethod.GET)
	public List<Entity> myscores(@Named("name") String name) {
		Query q = new Query("Score").setFilter(new FilterPredicate("name", FilterOperator.EQUAL, name)).addSort("score",
				SortDirection.DESCENDING);

		DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
		PreparedQuery pq = datastore.prepare(q);
		List<Entity> result = pq.asList(FetchOptions.Builder.withLimit(10));
		return result;
	}

	@ApiMethod(name = "addScore", httpMethod = HttpMethod.GET)
	public Entity addScore(@Named("score") int score, @Named("name") String name) {

		Entity e = new Entity("Score", "" + name + score);
		e.setProperty("name", name);
		e.setProperty("score", score);

		DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
		datastore.put(e);

		return e;
	}

	@ApiMethod(name = "postMessage", httpMethod = HttpMethod.POST)
	public Entity postMessage(PostMessage pm) {

		DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
		
		Query q = new Query("Profil").setFilter(new FilterPredicate("mail", FilterOperator.EQUAL, pm.owner));
		PreparedQuery pq = datastore.prepare(q);
		Entity result = pq.asSingleEntity();
		List<String> tab = new ArrayList<>();
		List<String> tab2 = new ArrayList<>();
		tab = (List<String>) result.getProperty("follower");
		tab2.add("");
		Entity e = new Entity("Post"); // quelle est la clef ?? non specifié -> clef automatique
		e.setProperty("owner", pm.owner);
		e.setProperty("url", pm.url);
		e.setProperty("body", pm.body);
		e.setProperty("to",tab);
		e.setProperty("likec", tab2);
		e.setProperty("date", new Date());

		
		Transaction txn = datastore.beginTransaction();
		datastore.put(e);
		txn.commit();
		return e;
	}
	
	@ApiMethod(name = "addprofil", httpMethod = HttpMethod.POST)
	public void addprofil(ProfilMessage Pm) {

		DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
		
		Query q = new Query("Profil").setFilter(new FilterPredicate("mail", FilterOperator.EQUAL, Pm.email));

		PreparedQuery pq = datastore.prepare(q);
		List<Entity> result = pq.asList(FetchOptions.Builder.withDefaults());
		
		if (result.isEmpty())
		{
			List<String> tab = new ArrayList<>();
			tab.add("");
			List<String> tab2 = new ArrayList<>();
			tab2.add(Pm.email);
			Entity e = new Entity("Profil"); // quelle est la clef ?? non specifié -> clef automatique
			e.setProperty("mail", Pm.email);
			e.setProperty("follow", tab);
			e.setProperty("follower", tab2);
			e.setProperty("pseudo", Pm.pseudo);

			Transaction txn = datastore.beginTransaction();
			datastore.put(e);
			txn.commit();
		}	
	}
	
	@ApiMethod(name = "followprofil", httpMethod = HttpMethod.POST)
	public Entity followprofil(FollowMessage Fm) {

		//Controle si la personne a follow a un profil connu dans la base
		DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
		
		Query q = new Query("Profil").setFilter(new FilterPredicate("mail", FilterOperator.EQUAL, Fm.mailFollow));

		PreparedQuery pq = datastore.prepare(q);
		List<Entity> result = pq.asList(FetchOptions.Builder.withDefaults());
		
		if (result.size()==1)//test si le profil est en base
		{
			//Ajout du follow dans le profil faisant la demande de follow
			Query q2 = new Query("Profil").setFilter(new FilterPredicate("mail", FilterOperator.EQUAL, Fm.mail));
			PreparedQuery pq2 = datastore.prepare(q2);
			Entity result2 = pq2.asSingleEntity();
			List<String> tab = new ArrayList<>();
			tab = (List<String>) result2.getProperty("follow");
			tab.add(Fm.mailFollow);
			result2.setProperty("follow", tab);
			Transaction txn = datastore.beginTransaction();
			datastore.put(result2);
			
			//Ajout du follow dans le profil qui a été follow
			Query q3 = new Query("Profil").setFilter(new FilterPredicate("mail", FilterOperator.EQUAL, Fm.mailFollow));
			PreparedQuery pq3 = datastore.prepare(q3);
			Entity result3 = pq3.asSingleEntity();
			List<String> tab2 = new ArrayList<>();
			tab2 = (List<String>) result3.getProperty("follower");
			tab2.add(Fm.mail);
			result3.setProperty("follower", tab2);
			datastore.put(result3);
			
			txn.commit();
			return(result2);
		}
		else
		{
			return null;
		}
	}
	
	@ApiMethod(name = "likeMessage", httpMethod = HttpMethod.POST)
	public Key likeMessage(User user, PostKey pk) {

		
		DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
		Key k = KeyFactory.createKey(pk.kind, pk.name);
		Query q = new Query("Post").setFilter(new FilterPredicate(Entity.KEY_RESERVED_PROPERTY, FilterOperator.EQUAL, k));
		PreparedQuery pq = datastore.prepare(q);
		Entity result = pq.asSingleEntity();
		List<String> tab = new ArrayList<>();
		tab = (List<String>) result.getProperty("likec");
		if (!tab.contains(user.getEmail()))
		{
			tab.add(user.getEmail());
			result.setProperty("likec", tab);
			datastore.put(result);

			
			Transaction txn = datastore.beginTransaction();
			txn.commit();
		}
		
		return k;
	}
	
	@ApiMethod(name = "cptlike", httpMethod = HttpMethod.POST)
	public List<String> cptlike(PostKey pk) {

		
		DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
		Key k = KeyFactory.createKey(pk.kind, pk.name);
		Query q = new Query("Post").setFilter(new FilterPredicate(Entity.KEY_RESERVED_PROPERTY, FilterOperator.EQUAL, k));
		PreparedQuery pq = datastore.prepare(q);
		Entity result = pq.asSingleEntity();
		List<String> tab = new ArrayList<>();
		tab = (List<String>)result.getProperty("likec");
		return tab;
	}
	
	@ApiMethod(name = "cptfollower", httpMethod = HttpMethod.POST)
	public List<String> cptfollower(User user) {

		DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
		Query q = new Query("Profil").setFilter(new FilterPredicate("mail", FilterOperator.EQUAL, user.getEmail()));

		PreparedQuery pq = datastore.prepare(q);
		Entity result = pq.asSingleEntity();
		List<String> tab = new ArrayList<>();
		tab = (List<String>)result.getProperty("follower");
		
		return tab;
	}
	
	@ApiMethod(name = "deleteMessage", httpMethod = HttpMethod.POST)
	public void deleteMessage(PostKey pk) {

		DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
		Key k = KeyFactory.createKey(pk.kind, pk.name);
		datastore.delete(k);
	}

	@ApiMethod(name = "mypost", httpMethod = HttpMethod.GET)
	public CollectionResponse<Entity> mypost(@Named("name") String name, @Nullable @Named("next") String cursorString) {

	    Query q = new Query("Post").setFilter(new FilterPredicate("owner", FilterOperator.EQUAL, name));

	    // https://cloud.google.com/appengine/docs/standard/python/datastore/projectionqueries#Indexes_for_projections
	    //q.addProjection(new PropertyProjection("body", String.class));
	    //q.addProjection(new PropertyProjection("date", java.util.Date.class));
	    //q.addProjection(new PropertyProjection("likec", Integer.class));
	    //q.addProjection(new PropertyProjection("url", String.class));

	    // looks like a good idea but...
	    // generate a DataStoreNeedIndexException -> 
	    // require compositeIndex on owner + date
	    // Explosion combinatoire.
	    // q.addSort("date", SortDirection.DESCENDING);
	    
	    DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
	    PreparedQuery pq = datastore.prepare(q);
	    
	    FetchOptions fetchOptions = FetchOptions.Builder.withLimit(2);
	    
	    if (cursorString != null) {
		fetchOptions.startCursor(Cursor.fromWebSafeString(cursorString));
		}
	    
	    QueryResultList<Entity> results = pq.asQueryResultList(fetchOptions);
	    cursorString = results.getCursor().toWebSafeString();
	    
	    return CollectionResponse.<Entity>builder().setItems(results).setNextPageToken(cursorString).build();
	    
	}
    
	@ApiMethod(name = "getPost",httpMethod = ApiMethod.HttpMethod.GET)
	public CollectionResponse<Entity> getPost(User user, @Nullable @Named("next") String cursorString)
			throws UnauthorizedException {

		if (user == null) {
			throw new UnauthorizedException("Invalid credentials");
		}

		//Query q = new Query("Post").setFilter(new FilterPredicate("owner", FilterOperator.EQUAL, user.getEmail()));
		Query q = new Query("Post").setFilter(new FilterPredicate("to", FilterOperator.EQUAL, user.getEmail())); //erreur 400 collection of element require

		// Multiple projection require a composite index
		// owner is automatically projected...
		// q.addProjection(new PropertyProjection("body", String.class));
		// q.addProjection(new PropertyProjection("date", java.util.Date.class));
		// q.addProjection(new PropertyProjection("likec", Integer.class));
		// q.addProjection(new PropertyProjection("url", String.class));

		// looks like a good idea but...
		// require a composite index
		// - kind: Post
		//  properties:
		//  - name: owner
		//  - name: date
		//    direction: desc

		// q.addSort("date", SortDirection.DESCENDING);

		DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
		PreparedQuery pq = datastore.prepare(q);

		FetchOptions fetchOptions = FetchOptions.Builder.withLimit(2);

		if (cursorString != null) {
			fetchOptions.startCursor(Cursor.fromWebSafeString(cursorString));
		}

		QueryResultList<Entity> results = pq.asQueryResultList(fetchOptions);
		cursorString = results.getCursor().toWebSafeString();

		return CollectionResponse.<Entity>builder().setItems(results).setNextPageToken(cursorString).build();
	}

	@ApiMethod(name = "postMsg", httpMethod = HttpMethod.POST)
	public Entity postMsg(User user, PostMessage pm) throws UnauthorizedException {

		if (user == null) {
			throw new UnauthorizedException("Invalid credentials");
		}
		
		DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
		
		Query q = new Query("Profil").setFilter(new FilterPredicate("mail", FilterOperator.EQUAL, user.getEmail()));
		PreparedQuery pq = datastore.prepare(q);
		Entity result = pq.asSingleEntity();
		List<String> tab = new ArrayList<>();
		List<String> tab2 = new ArrayList<>();
		List<String> tab3 = new ArrayList<>();
		tab3 = (List<String>)result.getProperty("follower");
		tab2.add("");

		Entity e = new Entity("Post", Long.MAX_VALUE-(new Date()).getTime()+":"+user.getEmail());
		e.setProperty("owner", user.getEmail());
		e.setProperty("url", pm.url);
		e.setProperty("body", pm.body);
		e.setProperty("to",tab3);
		e.setProperty("likec", tab2);
		e.setProperty("date", new Date());

///		Solution pour pas projeter les listes
//		Entity pi = new Entity("PostIndex", e.getKey());
//		HashSet<String> rec=new HashSet<String>();
//		pi.setProperty("receivers",rec);
		
		Transaction txn = datastore.beginTransaction();
		datastore.put(e);
//		datastore.put(pi);
		txn.commit();
		return result;
	}
}
