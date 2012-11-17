package com.nexussandbox;

import android.R.menu;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.facebook.FacebookActivity;
import com.facebook.LoginFragment;
import com.facebook.Session;
import com.facebook.SessionState;

public class MainActivity extends FacebookActivity {
	private static final String TAG = "NexusSandbox";
	
	private static final int SPLASH = 0;
	private static final int SELECTION = 1;
	private static final int SETTINGS = 2;
	private static final int FRAGMENT_COUNT = SETTINGS + 1;
	
	private Fragment[] fragments = new Fragment[FRAGMENT_COUNT];
	private static final String FRAGMENT_PREFIX = "fragment";
	
	private MenuItem settings;
	
	private boolean isResumed = false;
	
	private boolean restoredFragment = false;
	
	private String getBundleKey(int index) {
		return FRAGMENT_PREFIX + Integer.toString(index);
	}
	
	private void restoreFragment(Bundle savedInstanceState, int fragmentIndex) {
		Fragment fragment = null;
		if(savedInstanceState != null) {
			FragmentManager manager = getSupportFragmentManager();
	        fragment = manager.getFragment(savedInstanceState, 
	                   getBundleKey(fragmentIndex));
		}
		
		if(fragment != null) {
			fragments[fragmentIndex] = fragment;
			restoredFragment = true;
		} else {
			switch (fragmentIndex) {
			case SPLASH:
				fragments[SPLASH] = new SplashFragment();
				break;
			case SELECTION:
				fragments[SELECTION]= new SelectionFragment();
				break;
			case SETTINGS:
				fragments[SETTINGS] = new LoginFragment();
				break;

			default:
				Log.w(TAG, "Invalid fragment index");
				break;
			}
		}
	}
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        for(int i = 0; i < fragments.length; i++) {
        	restoreFragment(savedInstanceState, i);
        }
    }

//    @Override
//    public boolean onCreateOptionsMenu(Menu menu) {
//        getMenuInflater().inflate(R.menu.activity_main, menu);
//        return true;
//    }
    
    @Override
    public void onSaveInstanceState(Bundle outState) {
    	super.onSaveInstanceState(outState);
    	FragmentManager manager = getSupportFragmentManager();
    	Fragment f = manager.findFragmentById(R.id.body_frame);
    	
    	for(int i = 0; i < fragments.length; i++) {
    		if(fragments[i] == f) {
    			manager.putFragment(outState, getBundleKey(i), fragments[i]);
    		}
    	}    	
    }
    
    @Override
    protected void onSessionStateChange(SessionState state, Exception exception) {
    	if(isResumed) {
    		FragmentManager manager = getSupportFragmentManager();
    		int backStackSize = manager.getBackStackEntryCount();
    		
    		for(int i = 0; i < backStackSize; i++) {
    			manager.popBackStack();
    		}
    		
    		if(state.isOpened()) {
    			FragmentTransaction transaction = manager.beginTransaction();
    			transaction.replace(R.id.body_frame, fragments[SELECTION]).commit();
    		} else if(state.isClosed()) {
    			FragmentTransaction transaction = manager.beginTransaction();
    			transaction.replace(R.id.body_frame, fragments[SPLASH]).commit();
    		}
    	}
    }
    
    @Override
    protected void onResumeFragments() {
    	super.onResumeFragments();
    	Session session = Session.getActiveSession();
    	if(session == null || session.getState().isClosed()) {
    		session = new Session(this);
    		Session.setActiveSession(session);
    	}
    	
    	FragmentManager manager = getSupportFragmentManager();
    	
    	if(restoredFragment) {
    		return;
    	}
    	
    	// If we already have a valid token, then we can just open the session silently,
        // otherwise present the splash screen and ask the user to login.
    	if(session.getState().equals(SessionState.CREATED_TOKEN_LOADED)) {
    		// no need to add any fragments here since it will be 
            // handled in onSessionStateChange
    		session.openForRead(this);    		
    	} else if(session.isOpened()) {
    		// if the session is already open, try to show the selection fragment
    		Fragment fragment = manager.findFragmentById(R.id.body_frame);
    		
    		if(!(fragment instanceof SelectionFragment)) {
    			manager.beginTransaction().replace(R.id.body_frame, fragments[SELECTION]).commit();
    		}
    	} else {
    		FragmentTransaction transaction = manager.beginTransaction();
    		transaction.replace(R.id.body_frame, fragments[SPLASH]).commit();
    	}    	
    }
    
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {    	
    	super.onPrepareOptionsMenu(menu);
    	
    	FragmentManager manager = getSupportFragmentManager();
    	manager.executePendingTransactions();
    	Fragment currentFragment = manager.findFragmentById(R.id.body_frame);
    	
    	if(currentFragment == null) {
    		Log.d(TAG, "fragment=null");
    	} else if(currentFragment == fragments[SPLASH]) {
    		Log.d(TAG, "fragment=splash");
    	} else if(currentFragment == fragments[SETTINGS]) {
    		Log.d(TAG, "fragment=settings");
    	} else if(currentFragment == fragments[SELECTION]) {
    		Log.d(TAG, "fragment=selection");
    	}
    	
    	// only add the menu item when SELECTION fragment
    	if(currentFragment == fragments[SELECTION]) {
    		if(menu.size() == 0) {
    			settings = menu.add(R.string.settings);
    		}
    		return true;
    	} else {
    		menu.clear();
    		settings = null;
    	}
    	
    	return false;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
    	if(item.equals(settings)) {
    		FragmentManager manager = getSupportFragmentManager();
    		FragmentTransaction transaction = manager.beginTransaction();
    		transaction.add(R.id.body_frame, fragments[SETTINGS]).addToBackStack(null).commit();
    		
    		return true;
    	}
    	
    	return false;
    }
    
    @Override
    public void onResume() {
    	super.onResume();
    	isResumed = true;
    }
    
    @Override
    public void onPause() {
    	super.onPause();
    	isResumed = false;
    }
    
    public void testButton(View view) {
    	FragmentManager manager = getSupportFragmentManager();
    	Fragment fragment = manager.findFragmentById(R.id.body_frame);
    	
    	String fragmentId = "";
    	if(fragment == null) {
    		fragmentId = "null";
    	} else {
    		fragmentId = fragment.toString();
    	}
    	
    	Context context = getApplicationContext();
    	CharSequence text = "Current fragment is" + fragmentId;
    	int duration = Toast.LENGTH_SHORT;

    	Toast toast = Toast.makeText(context, text, duration);
    	toast.show();
    }
}
