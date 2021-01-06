package ca.uhn.example.base;

import ca.uhn.example.config.IgniteAppCfg;
import ca.uhn.example.model.VersionedId;

/*-
 * #%L
 * HAPI FHIR - Server Framework
 * %%
 * Copyright (C) 2014 - 2020 University Health Network
 * %%
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
 * #L%
 */

import ca.uhn.fhir.context.BaseRuntimeChildDefinition;
import ca.uhn.fhir.context.BaseRuntimeElementCompositeDefinition;
import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.interceptor.api.HookParams;
import ca.uhn.fhir.interceptor.api.IInterceptorBroadcaster;
import ca.uhn.fhir.interceptor.api.Pointcut;

import ca.uhn.fhir.model.api.IResource;
import ca.uhn.fhir.model.api.ResourceMetadataKeyEnum;
import ca.uhn.fhir.rest.annotation.ConditionalUrlParam;
import ca.uhn.fhir.rest.annotation.Create;
import ca.uhn.fhir.rest.annotation.Delete;
import ca.uhn.fhir.rest.annotation.History;
import ca.uhn.fhir.rest.annotation.IdParam;
import ca.uhn.fhir.rest.annotation.Read;
import ca.uhn.fhir.rest.annotation.RequiredParam;
import ca.uhn.fhir.rest.annotation.ResourceParam;
import ca.uhn.fhir.rest.annotation.Search;
import ca.uhn.fhir.rest.annotation.Update;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.api.server.IPreResourceAccessDetails;
import ca.uhn.fhir.rest.api.server.IPreResourceShowDetails;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.api.server.SimplePreResourceAccessDetails;
import ca.uhn.fhir.rest.api.server.SimplePreResourceShowDetails;
import ca.uhn.fhir.rest.api.server.storage.TransactionDetails;
import ca.uhn.fhir.rest.param.TokenAndListParam;
import ca.uhn.fhir.rest.param.TokenOrListParam;
import ca.uhn.fhir.rest.param.TokenParam;
import ca.uhn.fhir.rest.server.IResourceProvider;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import ca.uhn.fhir.rest.server.exceptions.ResourceGoneException;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import ca.uhn.fhir.rest.server.servlet.ServletRequestDetails;
import ca.uhn.fhir.util.ValidateUtil;
import com.google.common.collect.Lists;

import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteAtomicSequence;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.Ignition;
import org.apache.ignite.cache.query.QueryCursor;
import org.apache.ignite.cache.query.ScanQuery;
import org.apache.ignite.configuration.CacheConfiguration;
import org.apache.ignite.events.CacheEvent;
import org.apache.ignite.lang.IgniteBiPredicate;
import org.apache.ignite.lang.IgnitePredicate;
import org.hl7.fhir.instance.model.api.IBase;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IIdType;
import org.hl7.fhir.instance.model.api.IPrimitiveType;
import org.hl7.fhir.r4.model.BaseResource;
import org.hl7.fhir.r4.model.IdType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.cache.Cache.Entry;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;

import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.ignite.events.EventType.EVT_CACHE_OBJECT_PUT;
import static org.apache.ignite.events.EventType.EVT_CACHE_OBJECT_READ;
import static org.apache.ignite.events.EventType.EVT_CACHE_OBJECT_REMOVED;

/**
 * This class is a simple implementation of the resource provider
 * interface that uses a HashMap to store all resources in memory.
 * <p>
 * This class currently supports the following FHIR operations:
 * </p>
 * <ul>
 * <li>Create</li>
 * <li>Update existing resource</li>
 * <li>Update non-existing resource (e.g. create with client-supplied ID)</li>
 * <li>Delete</li>
 * <li>Search by resource type with no parameters</li>
 * </ul>
 *
 * @param <T> The resource type to support
 */
public class IgniteCacheR4ResourceProvider<T extends BaseResource> implements IResourceProvider {
	private static final Logger ourLog = LoggerFactory.getLogger(IgniteCacheR4ResourceProvider.class);
	private final Class<T> myResourceType;
	private final FhirContext myFhirContext;
	private final String myResourceName;
	
	// id生成器
	private IgniteAtomicSequence myNextId;
		
	//protected Map<String, TreeMap<Long, T>> myIdToVersionToResourceMap = Collections.synchronizedMap(new LinkedHashMap<>());
	// 持久化的存储
	protected IgniteCache<String,T> resourceMap;
	// 历史资源数据，可能会自动清除
	protected IgniteCache<VersionedId,T> versionResourceMap = null;
	// 某个资源instance的版本历史
	protected Map<String, LinkedList<VersionedId>> myIdToHistory = new ConcurrentHashMap<>();
	// 资源列表
	protected LinkedList<String> myTypeHistory = new LinkedList<>();
	
	protected AtomicLong mySearchCount = new AtomicLong(0);
	
	private AtomicLong myDeleteCount = new AtomicLong(0);
	private AtomicLong myUpdateCount = new AtomicLong(0);
	private AtomicLong myCreateCount = new AtomicLong(0);
	private AtomicLong myReadCount = new AtomicLong(0);
	

	/**
	 * Constructor
	 *
	 * @param theFhirContext  The FHIR context
	 * @param theResourceType The resource type to support
	 */
	public IgniteCacheR4ResourceProvider(FhirContext theFhirContext, Class<T> theResourceType,Ignite ignite) {
		myFhirContext = theFhirContext;
		myResourceType = theResourceType;
		myResourceName = myFhirContext.getResourceType(theResourceType);		
		init(ignite);
		clear();
	}
	
	/**
	 * Constructor
	 *
	 * @param theFhirContext  The FHIR context
	 * @param theResourceType The resource type to support
	 */
	public IgniteCacheR4ResourceProvider(FhirContext theFhirContext,Ignite ignite) {
		myFhirContext = theFhirContext;
		myResourceType = clazz(this);
		myResourceName = myFhirContext.getResourceType(myResourceType);
		init(ignite);
		clear();
	}
	
	private Class<T> clazz(Object foo){
		Type mySuperClass = foo.getClass().getGenericSuperclass();  
		if(mySuperClass instanceof ParameterizedType) {
			Type type = ((ParameterizedType)mySuperClass).getActualTypeArguments()[0]; 
			Class<T> tClass = (Class<T>)type;
			return tClass;
		}
		throw new java.lang.IllegalArgumentException("not have param type.");
	}
	
	

	private void init(Ignite ignite) {
		CacheConfiguration<String,T> cacheCfg = IgniteAppCfg.cacheConfigurationFor(myFhirContext,myResourceType);
		resourceMap = ignite.getOrCreateCache(cacheCfg);
		
		CacheConfiguration<VersionedId, T> historyCacheCfg = IgniteAppCfg.historyCacheConfigurationFor(myFhirContext,myResourceType);
		if(historyCacheCfg!=null) {
			this.versionResourceMap = ignite.getOrCreateCache(historyCacheCfg);
		}
		
		myNextId = ignite.atomicSequence(myResourceName, 0, true);
		
		// This optional local callback is called for each event notification
        // that passed remote predicate listener.
        IgnitePredicate<CacheEvent> locLsnr = new IgnitePredicate<CacheEvent>() {
            @Override public boolean apply(CacheEvent evt) {
                System.out.println("Received event [evt=" + evt.name() + ", key=" + evt.key() +
                    ", oldVal=" + evt.oldValue() + ", newVal=" + evt.newValue());

                return true; // Continue listening.
            }
        };
        

        // Subscribe to specified cache events on all nodes that have cache running.
        // Cache events are explicitly enabled in examples/config/example-ignite.xml file.
        ignite.events(ignite.cluster().forCacheNodes(cacheCfg.getName())).localListen(locLsnr, 
            EVT_CACHE_OBJECT_PUT, EVT_CACHE_OBJECT_READ, EVT_CACHE_OBJECT_REMOVED);
	}
	/**
	 * Clear all data held in this resource provider
	 */
	public void clear() {
		
		versionResourceMap.clear();
		resourceMap.clear();
		myIdToHistory.clear();
		myTypeHistory.clear();
	}

	/**
	 * Clear the counts used by {@link #getCountRead()} and other count methods
	 */
	public void clearCounts() {
		myReadCount.set(0L);
		myUpdateCount.set(0L);
		myCreateCount.set(0L);
		myDeleteCount.set(0L);
		mySearchCount.set(0L);
	}

	@Create
	public MethodOutcome create(@ResourceParam T theResource, RequestDetails theRequestDetails) {
		TransactionDetails transactionDetails = new TransactionDetails();

		createInternal(theResource, theRequestDetails, transactionDetails);

		myCreateCount.incrementAndGet();

		return new MethodOutcome()
			.setCreated(true)
			.setResource(theResource)
			.setId(theResource.getIdElement());
	}

	private void createInternal(@ResourceParam T theResource, RequestDetails theRequestDetails, TransactionDetails theTransactionDetails) {
		String idPartAsString = theResource.getIdElement().getIdPart();
		if(idPartAsString==null || idPartAsString.isEmpty()) {
			long idPart = myNextId.incrementAndGet();
			idPartAsString = Long.toString(idPart);
		}
		Long versionIdPart = VersionedId.nextVersion();

		IIdType id = store(theResource, idPartAsString, versionIdPart, theRequestDetails, theTransactionDetails);
		theResource.setId(id);
	}

	@Delete
	public MethodOutcome delete(@IdParam IdType theId, RequestDetails theRequestDetails) {
		TransactionDetails transactionDetails = new TransactionDetails();
		if(versionResourceMap!=null && theId.hasVersionIdPart()) {
			VersionedId vid = new VersionedId(theId);
			T versions = versionResourceMap.get(vid);
			if (versions == null || versions.isEmpty()) {
				throw new ResourceNotFoundException(theId);
			}
			versionResourceMap.remove(vid);
		}
		else {
			boolean rv = resourceMap.remove(theId.getIdPart());
		}

		long nextVersion = VersionedId.nextVersion();
		IIdType id = store(null, theId.getIdPart(), nextVersion, theRequestDetails, transactionDetails);

		myDeleteCount.incrementAndGet();

		return new MethodOutcome()
			.setId(id);
	}

	/**
	 * This method returns a simple operation count. This is mostly
	 * useful for testing purposes.
	 */
	public long getCountCreate() {
		return myCreateCount.get();
	}

	/**
	 * This method returns a simple operation count. This is mostly
	 * useful for testing purposes.
	 */
	public long getCountDelete() {
		return myDeleteCount.get();
	}

	/**
	 * This method returns a simple operation count. This is mostly
	 * useful for testing purposes.
	 */
	public long getCountRead() {
		return myReadCount.get();
	}

	/**
	 * This method returns a simple operation count. This is mostly
	 * useful for testing purposes.
	 */
	public long getCountSearch() {
		return mySearchCount.get();
	}

	/**
	 * This method returns a simple operation count. This is mostly
	 * useful for testing purposes.
	 */
	public long getCountUpdate() {
		return myUpdateCount.get();
	}

	@Override
	public Class<T> getResourceType() {
		return myResourceType;
	}

	

	@History
	public List<IBaseResource> historyInstance(@IdParam IIdType theId, RequestDetails theRequestDetails) {
		LinkedList<VersionedId> retVal = myIdToHistory.get(theId.getIdPart());
		if (retVal == null) {
			throw new ResourceNotFoundException(theId);
		}
		List<T> list = new ArrayList<>(retVal.size());
		for(VersionedId id: retVal) {
			T obj = this.versionResourceMap.get(id);
			if(obj!=null) {
				list.add(obj);
			}
		}

		return fireInterceptorsAndFilterAsNeeded(list, theRequestDetails);
	}

	@History
	public List<T> historyType() {
		List<T> list = new ArrayList<>(myTypeHistory.size());
		for(String id: myTypeHistory) {
			T obj = this.resourceMap.get(id);
			if(obj!=null) {
				list.add(obj);
			}
		}
		return list;
	}

	
	@Read(version = true)
	public T read(@IdParam IIdType theId, RequestDetails theRequestDetails) {		

		T retVal;
		if (theId.hasVersionIdPart() && versionResourceMap!=null) {			
			retVal = this.versionResourceMap.get(new VersionedId(theId));
			if (retVal==null) {
				throw new ResourceGoneException(theId);
			} 

		} else {
			retVal = resourceMap.get(theId.getIdPart());
			if (retVal == null) {
				throw new ResourceNotFoundException(theId);
			}
		}

		myReadCount.incrementAndGet();

		retVal = fireInterceptorsAndFilterAsNeeded(retVal, theRequestDetails);
		if (retVal == null) {
			throw new ResourceNotFoundException(theId);
		}
		return retVal;
	}

	@Search
	public List<IBaseResource> searchAll(RequestDetails theRequestDetails) {
		mySearchCount.incrementAndGet();
		List<T> retVal = getAllResources(null);
		return fireInterceptorsAndFilterAsNeeded(retVal, theRequestDetails);
	}

	@Nonnull
	protected List<T> getAllResources(IgniteBiPredicate<String,T> pred) {
		List<T> retVal = new ArrayList<>();
		QueryCursor<Entry<String, T>> cursor = this.getStoredResources(pred);
		for (Entry<String, T> next : cursor) {
			if (!next.getKey().isEmpty()) {
				T nextResource = next.getValue();
				if (nextResource != null) {
					retVal.add(nextResource);
				}
			}
		}

		return retVal;
	}

	@Search
	public List<IBaseResource> searchById(
		@RequiredParam(name = "_id") TokenAndListParam theIds, RequestDetails theRequestDetails) {

		List<T> retVal = new ArrayList<>();
		boolean matches = false;
		if (theIds != null && theIds.getValuesAsQueryTokens().size() > 0) {
			for (TokenOrListParam nextIdAnd : theIds.getValuesAsQueryTokens()) {
				matches = false;
				for (TokenParam nextOr : nextIdAnd.getValuesAsQueryTokens()) {
					String id = nextOr.getValue();
					T nextResource = this.resourceMap.get(id);
					if (nextResource!=null) {
						matches = true;
						retVal.add(nextResource);
					}
				}				
			}
		}

		if (!matches) {
			ourLog.info("search empty result!");
		}

		

		mySearchCount.incrementAndGet();

		return fireInterceptorsAndFilterAsNeeded(retVal, theRequestDetails);
	}

	/**
	 * 
	 * @param theResource
	 * @param theIdPart
	 * @param versionIdPart newVersion
	 * @param theRequestDetails
	 * @param theTransactionDetails
	 * @return
	 */
	private IIdType store(@ResourceParam T theResource, String theIdPart, Long versionIdPart, RequestDetails theRequestDetails, TransactionDetails theTransactionDetails) {
		IIdType id = myFhirContext.getVersion().newIdType();		
		id.setParts(null, myResourceName, theIdPart, versionIdPart.toString());
		if (theResource != null) {
			theResource.setId(id);
		}
		
		VersionedId vid = new VersionedId(theIdPart,versionIdPart);

		/*
		 * This is a bit of magic to make sure that the versionId attribute
		 * in the resource being stored accurately represents the version
		 * that was assigned by this provider
		 */
		if (theResource != null && this.versionResourceMap!=null) {
			BaseRuntimeChildDefinition metaChild = myFhirContext.getResourceDefinition(myResourceType).getChildByName("meta");
			List<IBase> metaValues = metaChild.getAccessor().getValues(theResource);
			if (metaValues.size() > 0) {
				IBase meta = metaValues.get(0);
				BaseRuntimeElementCompositeDefinition<?> metaDef = (BaseRuntimeElementCompositeDefinition<?>) myFhirContext.getElementDefinition(meta.getClass());
				BaseRuntimeChildDefinition versionIdDef = metaDef.getChildByName("versionId");
				List<IBase> versionIdValues = versionIdDef.getAccessor().getValues(meta);
				if (versionIdValues.size() > 0) {
					IPrimitiveType<?> versionId = (IPrimitiveType<?>) versionIdValues.get(0);
					versionId.setValueAsString(versionIdPart.toString());					
				}
			}
			// Store to ID+version->resource map
			this.versionResourceMap.put(vid, theResource);
		}

		ourLog.info("Storing resource with ID: {}", id.getValue());
		if(theResource!=null) {
			this.resourceMap.put(theIdPart, theResource);
		}

		if (theRequestDetails != null && this.versionResourceMap!=null) {
			IInterceptorBroadcaster interceptorBroadcaster = theRequestDetails.getInterceptorBroadcaster();

			if (theResource != null) {
				if (!myIdToHistory.containsKey(theIdPart)) {

					// Interceptor call: STORAGE_PRESTORAGE_RESOURCE_CREATED
					HookParams params = new HookParams()
						.add(RequestDetails.class, theRequestDetails)
						.addIfMatchesType(ServletRequestDetails.class, theRequestDetails)
						.add(IBaseResource.class, theResource)
						.add(TransactionDetails.class, theTransactionDetails);
					interceptorBroadcaster.callHooks(Pointcut.STORAGE_PRESTORAGE_RESOURCE_CREATED, params);
					interceptorBroadcaster.callHooks(Pointcut.STORAGE_PRECOMMIT_RESOURCE_CREATED, params);

				} else {
					T obj = this.versionResourceMap.get(myIdToHistory.get(theIdPart).getFirst());
					// Interceptor call: STORAGE_PRESTORAGE_RESOURCE_UPDATED
					HookParams params = new HookParams()
						.add(RequestDetails.class, theRequestDetails)
						.addIfMatchesType(ServletRequestDetails.class, theRequestDetails)
						.add(IBaseResource.class, obj)
						.add(IBaseResource.class, theResource)
						.add(TransactionDetails.class, theTransactionDetails);
					interceptorBroadcaster.callHooks(Pointcut.STORAGE_PRESTORAGE_RESOURCE_UPDATED, params);
					interceptorBroadcaster.callHooks(Pointcut.STORAGE_PRECOMMIT_RESOURCE_UPDATED, params);

				}
			}
		}

		// Store to type history map
		myTypeHistory.addFirst(id.getValue());

		// Store to ID history map
		myIdToHistory.computeIfAbsent(theIdPart, t -> new LinkedList<>());
		myIdToHistory.get(theIdPart).addFirst(vid);

		// Return the newly assigned ID including the version ID
		return id;
	}

	/**
	 * @param theConditional This is provided only so that subclasses can implement if they want
	 */
	@Update
	public MethodOutcome update(
		@ResourceParam T theResource,
		@ConditionalUrlParam String theConditional,
		RequestDetails theRequestDetails) {
		TransactionDetails transactionDetails = new TransactionDetails();

		ValidateUtil.isTrueOrThrowInvalidRequest(isBlank(theConditional), "This server doesn't support conditional update");

		boolean created = updateInternal(theResource, theRequestDetails, transactionDetails);
		myUpdateCount.incrementAndGet();

		return new MethodOutcome()
			.setCreated(created)
			.setResource(theResource)
			.setId(theResource.getIdElement());
	}

	private boolean updateInternal(@ResourceParam T theResource, RequestDetails theRequestDetails, TransactionDetails theTransactionDetails) {
		String idPartAsString = theResource.getIdElement().getIdPart();
		
		Long versionIdPart;
		boolean created;
		
		if(this.versionResourceMap!=null && theResource.getIdElement().hasVersionIdPart()) {
			VersionedId vid = new VersionedId(theResource.getIdElement());
			T versionResource = this.versionResourceMap.get(vid);
			
			if (versionResource==null) {
				versionIdPart = 1L;
				created = true;
			} else {				
				created = false;
			}
		}
		else {
			T versionResource = this.resourceMap.get(idPartAsString);
			
			if (versionResource==null) {
				versionIdPart = 1L;
				created = true;
			} else {				
				created = false;
			}
		}	

		versionIdPart = VersionedId.nextVersion();
		IIdType id = store(theResource, idPartAsString, versionIdPart, theRequestDetails, theTransactionDetails);
		theResource.setId(id);
		return created;
	}

	public FhirContext getFhirContext() {
		return myFhirContext;
	}

	/**
	 * This is a utility method that can be used to store a resource without
	 * having to use the outside API. In this case, the storage happens without
	 * any interaction with interceptors, etc.
	 *
	 * @param theResource The resource to store. If the resource has an ID, that ID is updated.
	 * @return Return the ID assigned to the stored resource
	 */
	public IIdType store(T theResource) {
		if (theResource.getIdElement().hasIdPart()) {
			updateInternal(theResource, null, new TransactionDetails());
		} else {
			createInternal(theResource, null, new TransactionDetails());
		}
		return theResource.getIdElement();
	}

	/**
	 * Returns an unmodifiable list containing the current version of all resources stored in this provider
	 *
	 * @since 4.1.0
	 */
	public QueryCursor<Entry<String, T>> getStoredResources(IgniteBiPredicate<String,T> pred) {
		List<T> retVal = new ArrayList<>();
		ScanQuery<String,T> scan = new ScanQuery<>(pred);		
		return this.resourceMap.query(scan);
	}

	private static <T extends IBaseResource> T fireInterceptorsAndFilterAsNeeded(T theResource, RequestDetails theRequestDetails) {
		List<IBaseResource> output = fireInterceptorsAndFilterAsNeeded(Lists.newArrayList(theResource), theRequestDetails);
		if (output.size() == 1) {
			return theResource;
		} else {
			return null;
		}
	}

	protected static <T extends IBaseResource> List<IBaseResource> fireInterceptorsAndFilterAsNeeded(List<T> theResources, RequestDetails theRequestDetails) {
		List<IBaseResource> resourcesToReturn = new ArrayList<>(theResources);

		if (theRequestDetails != null) {
			IInterceptorBroadcaster interceptorBroadcaster = theRequestDetails.getInterceptorBroadcaster();

			// Call the STORAGE_PREACCESS_RESOURCES pointcut (used for consent/auth interceptors)
			SimplePreResourceAccessDetails preResourceAccessDetails = new SimplePreResourceAccessDetails(resourcesToReturn);
			HookParams params = new HookParams()
				.add(RequestDetails.class, theRequestDetails)
				.addIfMatchesType(ServletRequestDetails.class, theRequestDetails)
				.add(IPreResourceAccessDetails.class, preResourceAccessDetails);
			interceptorBroadcaster.callHooks(Pointcut.STORAGE_PREACCESS_RESOURCES, params);
			preResourceAccessDetails.applyFilterToList();

			// Call the STORAGE_PREACCESS_RESOURCES pointcut (used for consent/auth interceptors)
			SimplePreResourceShowDetails preResourceShowDetails = new SimplePreResourceShowDetails(resourcesToReturn);
			HookParams preShowParams = new HookParams()
				.add(RequestDetails.class, theRequestDetails)
				.addIfMatchesType(ServletRequestDetails.class, theRequestDetails)
				.add(IPreResourceShowDetails.class, preResourceShowDetails);
			interceptorBroadcaster.callHooks(Pointcut.STORAGE_PRESHOW_RESOURCES, preShowParams);
			resourcesToReturn = preResourceShowDetails.toList();

		}

		return resourcesToReturn;
	}
	

	@Search
	public List<IBaseResource> searchByParams(
			@ResourceParam T sample, RequestDetails theRequestDetails) {

		List<T> retVal = new ArrayList<>();
		boolean matches = false;
		if (sample != null && !sample.isEmpty()) {
			
			List<T> retValFirst = getAllResources(null);
			
			
			for (T nextResource : retValFirst) {
				matches = false;
				for (java.util.Map.Entry<String, String[]> nextOr : theRequestDetails.getParameters().entrySet()) {
					String field = nextOr.getKey();
				
					if (nextResource!=null) {						
						matches = true;
						retVal.add(nextResource);
					}
				}				
			}
		}

		if (!matches) {
			ourLog.info("search empty result!");
		}

		

		mySearchCount.incrementAndGet();

		return fireInterceptorsAndFilterAsNeeded(retVal, theRequestDetails);
	}

}
