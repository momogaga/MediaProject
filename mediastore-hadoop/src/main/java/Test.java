import org.shiftone.cache.Cache;
import org.shiftone.cache.CacheConfiguration;
import org.shiftone.cache.CacheFactory;
import org.shiftone.cache.ConfigurationException;
import org.shiftone.cache.policy.lfu.LfuCacheFactory;

public class Test {

	public static void main(String[] args) {

		// CacheConfiguration config;

		// config = new CacheConfiguration();
		// CacheFactory factory = config.getCacheFactory("default");
		// Cache cache = factory.newInstance(
		// "news.sports.football",
		// 1, 500);

		Cache cache = new LfuCacheFactory().newInstance("testSimple", 1000, 5);

		cache.addObject(1100, "1100");
		String s = (String) cache.getObject(1100);
		System.out.println(s);
		for (int i = 0; i < 1000; i++) {
			cache.addObject(i, "" + i);
			// cache.getObject(11);
			// cache.getObject(11);
		}

		for (int i = 0; i < 1000; i++) {
			// cache.addObject(i, ""+i);
			s = (String) cache.getObject(1100);
			System.out.println(s);
			// cache.getObject(11);
			// cache.getObject(11);
		}

	}

}
