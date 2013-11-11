package ch.ethz.inf.vs.californium.network.config;

public interface NetworkConfigObserver {

	public void changed(String key, Object value);
	public void changed(String key, String value);
	public void changed(String key, int value);
	public void changed(String key, long value);
	public void changed(String key, float value);
	public void changed(String key, double value);
	public void changed(String key, boolean value);
	
}

