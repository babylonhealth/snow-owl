/*
 * Copyright 2011-2015 B2i Healthcare Pte Ltd, http://b2i.sg
 * 
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
 */
package com.b2international.snowowl.core;

import static com.google.common.base.Strings.nullToEmpty;
import static com.google.common.collect.Sets.newHashSet;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.Platform;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.b2international.commons.ClassUtils;
import com.b2international.commons.CompareUtils;
import com.b2international.commons.StringUtils;
import com.b2international.snowowl.core.api.ExtendedComponent;
import com.b2international.snowowl.core.api.IBranchPath;
import com.b2international.snowowl.core.api.IComponent;
import com.b2international.snowowl.core.api.IComponentIconIdProvider;
import com.b2international.snowowl.core.api.ILookupService;
import com.b2international.snowowl.core.api.IMappingSetMembershipLookupService;
import com.b2international.snowowl.core.api.INameProviderFactory;
import com.b2international.snowowl.core.api.ISearchResultProvider;
import com.b2international.snowowl.core.api.ITerminologyComponentIdProvider;
import com.b2international.snowowl.core.api.IValueSetMembershipLookupService;
import com.b2international.snowowl.core.api.browser.IClientTerminologyBrowser;
import com.b2international.snowowl.core.api.browser.IClientTerminologyBrowserFactory;
import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import com.google.common.collect.Sets;

/**
 *
 */
public class CoreTerminologyBroker {

	private static final Logger LOGGER = LoggerFactory.getLogger(CoreTerminologyBroker.class);

	/**
	 * Represents an available terminology with a human readable name.
	 */
	public static interface ICoreTerminologyInformation {
		/**
		 * Returns with a human readable name of this instance.
		 * @return the human readable name.
		 */
		String getName();

		/**
		 * Returns with the unique identifier of the terminology.
		 * @return the unique terminology ID.
		 */
		String getId();
	}

	/**
	 * Represents a terminology component with a unique identifier, some human readable name.
	 */
	public static interface ICoreTerminologyComponentInformation extends ICoreTerminologyInformation {
		/**
		 * Returns with the unique identifier of this component.
		 * @return the unique ID.
		 */
		@Override
		String getId();
	}

	public static final String UNSPECIFIED = "UNSPECIFIED";
	public static final int UNSPECIFIED_NUMBER = -1;
	public static final short UNSPECIFIED_NUMBER_SHORT = -1;
	
	private static final ICoreTerminologyComponentInformation UNSPECIFIED_COMPONENT = new ICoreTerminologyComponentInformation() {
		@Override public String getName() {	return UNSPECIFIED;	}
		@Override public String getId() { return UNSPECIFIED; }
	};

	public static final String TERMINOLOGY_COMPONENT_EXTENSION_POINT_ID = "com.b2international.snowowl.core.terminologyComponent";
	public static final String TERMINOLOGY_EXTENSION_POINT_ID = "com.b2international.snowowl.core.terminology";
	public static final String REPRESENTATION_EXTENSION_POINT_ID = "com.b2international.snowowl.core.representation";
	public static final String SEARCH_RESULT_PROVIDER_EXTENSION_POINT_ID = "com.b2international.snowowl.core.searchResultProvider";
	public static final String REFSET_MEMBERSHIP_LOOKUP_SERVICE_EXTENSION_POINT_ID = "com.b2international.snowowl.core.refSetMembershipLookupService";
	public static final String VALUE_SET_MEMBERSHIP_LOOKUP_SERVICE_EXTENSION_POINT_ID = "com.b2international.snowowl.core.valueSetMembershipLookupService";
	public static final String MAPPING_SET_MEMBERSHIP_LOOKUP_SERVICE_EXTENSION_POINT_ID = "com.b2international.snowowl.core.mappingSetMembershipLookupService";
	public static final String TERMINOLOGY_BROWSER_FACTORY_EXTENSION_POINT_ID = "com.b2international.snowowl.core.terminologyBrowserFactory";
	public static final String LOOKUP_SERVICE_EXTENSION_POINT_ID = "com.b2international.snowowl.core.lookupService";
	public static final String COMPONENT_ICON_ID_PROVIDER_EXTENSION_POINT_ID = "com.b2international.snowowl.core.componentIconIdProvider";
	public static final String NAME_PROVIDER_SERVICE_EXTENSION_POINT_ID = "com.b2international.snowowl.core.nameProviderFactory";
	public static final String TERMINOLOGY_ID_ATTRIBUTE = "terminologyId";
	public static final String TERMINOLOGY_COMPONENT_ID_ATTRIBUTE = "terminologyComponentId";
	public static final String PRIMARY_COMPONENT_ID_ATTRIBUTE = "primaryComponentId";
	public static final String CLASS_ATTRIBUTE = "class";
	public static final String ID_ATTRIBUTE = "id";
	public static final String OID_ATTRIBUTE = "oid";
	public static final String SUPPORTS_EFFECTIVE_TIME_ATTRIBUTE = "supportsEffectiveTime";
	public static final String INT_ID_ATTRIBUTE = "intId";
	public static final String HIERARCHICAL_ATTRIBUTE = "hierarchical";
	public static final String TOP_LEVEL_ATTRIBUTE = "topLevel";
	public static final String NAME_ATTRIBUTE = "name";

	private static final Map<Integer, String> INT_TO_ID_CACHE = Maps.newConcurrentMap();
	private static final Map<String, Integer> ID_TO_INT_CACHE = Maps.newConcurrentMap();
	private static final Map<String, Short> ID_TO_SHORT_CACHE = Maps.newConcurrentMap();
	private static final HashMultimap<Short, String> SHORT_TO_CLASS_CACHE = HashMultimap.<Short, String> create();
	private static final Map<Class<?>, Integer> CLASS_TO_INT_CACHE = Maps.newConcurrentMap();
	private static final Map<Class<?>, String> CLASS_TO_ID_CACHE = Maps.newConcurrentMap();
	private static final Map<Class<?>, Short> CLASS_TO_SHORT_CACHE = Maps.newConcurrentMap();

	private static CoreTerminologyBroker instance;
	private Map<String, ICoreTerminologyComponentInformation> registeredTerminologyComponents;
	private Supplier<Map<String, ICoreTerminologyInformation>> registeredTerminologiesSupplier = Suppliers.memoize(new Supplier<Map<String, ICoreTerminologyInformation>>() {
		@Override public Map<String, ICoreTerminologyInformation> get() {
			
			final Map<String, ICoreTerminologyInformation> $ = Maps.newHashMap();
			
			for (final IConfigurationElement terminology : Platform.getExtensionRegistry().getConfigurationElementsFor(TERMINOLOGY_EXTENSION_POINT_ID)) {
				
				final String id = Preconditions.checkNotNull(terminology.getAttribute(ID_ATTRIBUTE));
				final String name = Preconditions.checkNotNull(terminology.getAttribute(NAME_ATTRIBUTE));
				final ICoreTerminologyInformation info = new ICoreTerminologyInformation() {
					@Override public String getName() { return name; }
					@Override public String getId() { return id; }
				};
				$.put(id, info);
			}
			
			return Collections.unmodifiableMap($);
		}
	});
	
	private Supplier<Map<String, String>> primaryTerminologyComponentSupplier = Suppliers.memoize(new Supplier<Map<String, String>>() {
		@Override public Map<String, String> get() {
			final Map<String, String> $ = Maps.newHashMap();
			for (final IConfigurationElement terminology : Platform.getExtensionRegistry().getConfigurationElementsFor(TERMINOLOGY_EXTENSION_POINT_ID)) {
				final String id = Preconditions.checkNotNull(terminology.getAttribute(ID_ATTRIBUTE));
				final String primaryComponentId = Preconditions.checkNotNull(terminology.getAttribute(PRIMARY_COMPONENT_ID_ATTRIBUTE));
				$.put(id, primaryComponentId);
			}
			return Collections.unmodifiableMap($);

		}
	});
	
	/**Supplies multimapping between a terminology and all terminology components registered for the terminology.*/
	private Supplier<Multimap<String, String>> terminologyToComponentsSupplier = Suppliers.memoize(new Supplier<Multimap<String, String>>() {
		@Override public Multimap<String, String> get() {
			final Multimap<String,String> $ = HashMultimap.create(); 
			for (final IConfigurationElement component : Platform.getExtensionRegistry().getConfigurationElementsFor(TERMINOLOGY_COMPONENT_EXTENSION_POINT_ID)) {
				$.put(
						Preconditions.checkNotNull(component.getAttribute(TERMINOLOGY_ID_ATTRIBUTE)),
						Preconditions.checkNotNull(component.getAttribute(ID_ATTRIBUTE)));
			}
			return Multimaps.unmodifiableMultimap($);
		}
	});
	
	/**Supplies mapping between terminology component IDs and the corresponding terminology ID.*/
	private Supplier<Map<String, String>> componentToTerminologySupplier = Suppliers.memoize(new Supplier<Map<String, String>>() {
		@Override public Map<String, String> get() {
			final Map<String,String> $ = Maps.newHashMap(); 
			for (final IConfigurationElement component : Platform.getExtensionRegistry().getConfigurationElementsFor(TERMINOLOGY_COMPONENT_EXTENSION_POINT_ID)) {
				$.put(
						Preconditions.checkNotNull(component.getAttribute(ID_ATTRIBUTE)),
						Preconditions.checkNotNull(component.getAttribute(TERMINOLOGY_ID_ATTRIBUTE)));
			}
			return Collections.unmodifiableMap($);
		}
	});

	private CoreTerminologyBroker() {
		//avoid instantiation
	}

	public static CoreTerminologyBroker getInstance() {
		if (instance == null) {
			synchronized (CoreTerminologyBroker.class) {
				if (instance == null)
					instance = new CoreTerminologyBroker();
			}
		}
		return instance;
	}

	/**
	 * Returns with the terminology independent {@link IComponent component} representation of the specified object after adapting it.
	 * Returns with {@code null} if the object cannot be adapted to any terminology independent component.
	 * <br>This method swallows all the exceptions occurred during the adaption process and returns with {@code null}.
	 * @param object the object to adapt. Cannot be {@code null}.
	 * @return the terminology independent component.
	 */
	public IComponent<?> adapt(final Object object) {
		Preconditions.checkNotNull(object, "Object argument cannot be null.");
		try {
			return (IComponent<?>) Platform.getAdapterManager().getAdapter(object, IComponent.class);
		} catch (final Throwable t) {
			LOGGER.error("Error while adapting object: " + object, t);
			return null;
		}
	}

	public synchronized Set<String> getClassesForComponentId(final short terminologyComponentId) {

		if (UNSPECIFIED_NUMBER_SHORT == terminologyComponentId) {
			return Collections.emptySet();
		}

		final Set<String> classes = SHORT_TO_CLASS_CACHE.get(terminologyComponentId);
		if (!CompareUtils.isEmpty(classes)) {
			return classes;
		}

		final Set<String> representationClasses = getClassesForComponentId(getTerminologyComponentId(terminologyComponentId));
		
		for (String representationClass : representationClasses) {
			SHORT_TO_CLASS_CACHE.put(terminologyComponentId, representationClass);
		}
		return SHORT_TO_CLASS_CACHE.get(terminologyComponentId);
	}

	public synchronized Set<String> getClassesForComponentId(final String componentId) {
		final Set<String> representationClasses = newHashSet();
		for (final IConfigurationElement element : Platform.getExtensionRegistry().getConfigurationElementsFor(REPRESENTATION_EXTENSION_POINT_ID)) {

			if (componentId.equals(String.valueOf(element.getAttribute(TERMINOLOGY_COMPONENT_ID_ATTRIBUTE)))) {
				representationClasses.add(element.getAttribute(CLASS_ATTRIBUTE));
			}

		}
		return representationClasses;
	}

	public String getTerminologyComponentId(final Object object) {
		Preconditions.checkNotNull(object, "Object argument cannot be null.");

		String terminologyComponentId = CLASS_TO_ID_CACHE.get(object.getClass());
		if (null != terminologyComponentId) {
			return terminologyComponentId;
		} else if (object instanceof ExtendedComponent) {
			return getTerminologyComponentId(((ExtendedComponent) object).getTerminologyComponentId());
		} else if (object instanceof ITerminologyComponentIdProvider) {
			return ((ITerminologyComponentIdProvider) object).getTerminologyComponentId();
		}

		for (final IConfigurationElement element : Platform.getExtensionRegistry().getConfigurationElementsFor(REPRESENTATION_EXTENSION_POINT_ID)) {
			final String representationClass = element.getAttribute(CLASS_ATTRIBUTE);
			if (ClassUtils.isClassAssignableFrom(object.getClass(), representationClass)) {
				terminologyComponentId = element.getAttribute(TERMINOLOGY_COMPONENT_ID_ATTRIBUTE);
				CLASS_TO_ID_CACHE.put(object.getClass(), terminologyComponentId);
				return terminologyComponentId;
			}
		}
		throw new IllegalArgumentException("No terminology component extension has been registered for the passed in object: " + object.getClass());
	}

	public short getTerminologyComponentIdAsShort(final Object object) {
		Preconditions.checkNotNull(object, "Object argument cannot be null.");

		Short id = CLASS_TO_SHORT_CACHE.get(object.getClass());
		if (null != id) {
			return id.shortValue();
		} else if (object instanceof ExtendedComponent) {
			return ((ExtendedComponent) object).getTerminologyComponentId();
		}

		final int asInt = getTerminologyComponentIdAsInt(object);
		if (asInt < Integer.MIN_VALUE || asInt > Integer.MAX_VALUE)
			throw new NumberFormatException("Value out of range. Value:\"" + asInt);
		id = Short.valueOf((short) asInt);
		CLASS_TO_SHORT_CACHE.put(object.getClass(), id);
		return (short) asInt;
	}

	public short getTerminologyComponentIdAsShort(final String terminologyComponentId) {
		Preconditions.checkNotNull(terminologyComponentId, "Terminology component identifier argument cannot be null.");
		Preconditions.checkArgument(!StringUtils.isEmpty(terminologyComponentId), "Terminology component identifier argument cannot be empty string.");

		Short componentId = ID_TO_SHORT_CACHE.get(terminologyComponentId);
		if (null != componentId) {
			return componentId.shortValue();
		}

		final int asInt = getTerminologyComponentIdAsInt(terminologyComponentId);
		if (asInt < Integer.MIN_VALUE || asInt > Integer.MAX_VALUE)
			throw new NumberFormatException("Value out of range. Value:\"" + asInt);
		componentId = Short.valueOf((short) asInt);
		ID_TO_SHORT_CACHE.put(terminologyComponentId, componentId);
		return (short) asInt;
	}

	public String getTerminologyComponentId(final short shortId) {
		return getTerminologyComponentId((int) shortId);
	}

	public int getTerminologyComponentIdAsInt(final Object object) {
		Preconditions.checkNotNull(object, "Object argument cannot be null.");

		final Integer componentId = CLASS_TO_INT_CACHE.get(object.getClass());
		if (null != componentId) {
			return componentId.intValue();
		} else if (object instanceof ExtendedComponent) {
			return (int) ((ExtendedComponent) object).getTerminologyComponentId();
		}

		for (final IConfigurationElement element : Platform.getExtensionRegistry().getConfigurationElementsFor(REPRESENTATION_EXTENSION_POINT_ID)) {
			final String representationClass = element.getAttribute(CLASS_ATTRIBUTE);
			if (ClassUtils.isClassAssignableFrom(object.getClass(), representationClass)) {
				final int idAsInt = getTerminologyComponentIdAsInt(element.getAttribute(TERMINOLOGY_COMPONENT_ID_ATTRIBUTE));
				CLASS_TO_INT_CACHE.put(object.getClass(), Integer.valueOf(idAsInt));
				return idAsInt;
			}
		}
		throw new IllegalArgumentException("No terminology component extension has been registered for the passed in object: " + object.getClass());
	}

	public String getTerminologyComponentId(final int intId) {
		if (UNSPECIFIED_NUMBER == intId)
			return UNSPECIFIED;

		String componentId = INT_TO_ID_CACHE.get(Integer.valueOf(intId));
		if (null != componentId) {
			return componentId;
		}

		for (final IConfigurationElement element : Platform.getExtensionRegistry().getConfigurationElementsFor(TERMINOLOGY_COMPONENT_EXTENSION_POINT_ID)) {
			if (intId == Integer.parseInt(element.getAttribute(INT_ID_ATTRIBUTE))) {
				componentId = element.getAttribute(ID_ATTRIBUTE);
				INT_TO_ID_CACHE.put(Integer.valueOf(intId), componentId);
				return componentId;
			}
		}
		throw new IllegalArgumentException("No terminology component extension has been registered with the passed in integer ID: " + intId);
	}

	public int getTerminologyComponentIdAsInt(final String terminologyComponentId) {
		Preconditions.checkNotNull(terminologyComponentId, "Terminology component identifier argument cannot be null.");
		Preconditions.checkArgument(!StringUtils.isEmpty(terminologyComponentId), "Terminology component identifier argument cannot be empty string.");
		if (UNSPECIFIED.equals(terminologyComponentId))
			return UNSPECIFIED_NUMBER;

		Integer id = ID_TO_INT_CACHE.get(terminologyComponentId);
		if (null != id) {
			return id.intValue();
		}

		for (final IConfigurationElement terminologyComponentElement : Platform.getExtensionRegistry().getConfigurationElementsFor(TERMINOLOGY_COMPONENT_EXTENSION_POINT_ID)) {
			if (terminologyComponentId.equals(terminologyComponentElement.getAttribute(ID_ATTRIBUTE))) {
				id = Integer.valueOf(Integer.parseInt(terminologyComponentElement.getAttribute(INT_ID_ATTRIBUTE)));
				ID_TO_INT_CACHE.put(terminologyComponentId, id);
				return id.intValue();
			}
		}
		throw new IllegalArgumentException("No terminology component extension has been registered with the passed in ID: " + terminologyComponentId);
	}

	public String getTerminologyId(final Object object) {
		Preconditions.checkNotNull(object, "Object argument cannot be null.");

		for (final IConfigurationElement element : Platform.getExtensionRegistry().getConfigurationElementsFor(REPRESENTATION_EXTENSION_POINT_ID)) {
			final String representationClass = element.getAttribute(CLASS_ATTRIBUTE);
			if (ClassUtils.isClassAssignableFrom(object.getClass(), representationClass)) {
				final String terminologyComponentId = element.getAttribute(TERMINOLOGY_COMPONENT_ID_ATTRIBUTE);

				//iterate through all registered terminology component extensions
				for (final IConfigurationElement terminologComponents : Platform.getExtensionRegistry().getConfigurationElementsFor(TERMINOLOGY_COMPONENT_EXTENSION_POINT_ID)) {
					if (terminologyComponentId.equals(terminologComponents.getAttribute(ID_ATTRIBUTE))) {
						return terminologComponents.getAttribute(TERMINOLOGY_ID_ATTRIBUTE);
					}
				}
			}
		}
		throw new IllegalArgumentException("No terminology extension has been registered for the passed in object: " + object.getClass());
	}

	public String getTerminologyOid(final Object object) {
		Preconditions.checkNotNull(object, "Object argument cannot be null.");

		for (final IConfigurationElement element : Platform.getExtensionRegistry().getConfigurationElementsFor(REPRESENTATION_EXTENSION_POINT_ID)) {
			final String representationClass = element.getAttribute(CLASS_ATTRIBUTE);
			if (ClassUtils.isClassAssignableFrom(object.getClass(), representationClass)) {
				final String terminologyComponentId = element.getAttribute(TERMINOLOGY_COMPONENT_ID_ATTRIBUTE);
				return getTerminologyOidByTerminologyComponentId(terminologyComponentId);
			}
		}
		throw new IllegalArgumentException("No terminology extension has been registered for the passed in object: " + object.getClass());
	}

	public String getTerminologyOidByTerminologyComponentId(final String terminologyComponentId) {
		//iterate through all registered terminology component extensions
		for (final IConfigurationElement terminologComponents : Platform.getExtensionRegistry().getConfigurationElementsFor(TERMINOLOGY_COMPONENT_EXTENSION_POINT_ID)) {
			if (terminologyComponentId.equals(terminologComponents.getAttribute(ID_ATTRIBUTE))) {
				final String terminologyId = terminologComponents.getAttribute(TERMINOLOGY_ID_ATTRIBUTE);
				for (final IConfigurationElement terminology : Platform.getExtensionRegistry().getConfigurationElementsFor(TERMINOLOGY_EXTENSION_POINT_ID)) {
					if (terminologyId.equals(terminology.getAttribute(ID_ATTRIBUTE))) {
						return terminology.getAttribute(OID_ATTRIBUTE);
					}
				}
			}
		}
		throw new IllegalArgumentException("No terminology extension has been registered for the passed in terminology component id: " + terminologyComponentId);
	}

	public int getTopLevelDepth(final String terminologyId) {
		Preconditions.checkNotNull(terminologyId, "Terminology identifier argument cannot be null.");
		Preconditions.checkArgument(!StringUtils.isEmpty(terminologyId), "Terminology identifier argument cannot be empty string.");
		//iterate through all registered terminology component extensions
		for (final IConfigurationElement terminology : Platform.getExtensionRegistry().getConfigurationElementsFor(TERMINOLOGY_EXTENSION_POINT_ID)) {
			if (terminologyId.equals(terminology.getAttribute(ID_ATTRIBUTE))) {
				final String topLevelAttribute = terminology.getAttribute(TOP_LEVEL_ATTRIBUTE);
				try {
					return Integer.parseInt(topLevelAttribute);
				} catch (final NumberFormatException e) {
					throw new RuntimeException("Terminology level depth was not an integer. Top level depth: " + topLevelAttribute + " for terminology: " + terminologyId);
				}
			}
		}
		throw new RuntimeException("No top level depth was registered for " + terminologyId);
	}

	public int getTopLevelDepth(final Object object) {
		return getTopLevelDepth(getTerminologyId(object));
	}

	public boolean isHierarchical(final String terminologyComponentId) {
		Preconditions.checkNotNull(terminologyComponentId, "Terminology component identifier argument cannot be null.");
		Preconditions.checkArgument(!StringUtils.isEmpty(terminologyComponentId), "Terminology component identifier argument cannot be empty string.");

		//iterate through all registered terminology component extensions
		for (final IConfigurationElement terminologComponents : Platform.getExtensionRegistry().getConfigurationElementsFor(TERMINOLOGY_COMPONENT_EXTENSION_POINT_ID)) {
			if (terminologyComponentId.equals(terminologComponents.getAttribute(ID_ATTRIBUTE))) {
				return Boolean.valueOf(terminologComponents.getAttribute(HIERARCHICAL_ATTRIBUTE));
			}
		}
		throw new IllegalArgumentException("No terminology component extension has been registered with the passed in ID: " + terminologyComponentId);
	}

	public boolean isHierarchical(final Object object) {
		return isHierarchical(getTerminologyComponentId(object));
	}

//	public Collection<IRefSetMembershipLookupService<String>> getRefSetMembershipLookupServices() {
//		final Set<IRefSetMembershipLookupService<String>> searchers = Sets.newHashSet();
//		final Iterator<IConfigurationElement> terminologyElements = Iterators.forArray(Platform.getExtensionRegistry().getConfigurationElementsFor(TERMINOLOGY_EXTENSION_POINT_ID));
//		final Set<String> allTerminologyIds = Sets.newHashSet(Iterators.transform(terminologyElements, new Function<IConfigurationElement, String>() {
//
//			//implements com.google.common.base.Function<org.eclipse.core.runtime.IConfigurationElement,java.lang.String>.apply
//			@Override
//			public String apply(final IConfigurationElement configurationElement) {
//				return configurationElement.getAttribute(ID_ATTRIBUTE);
//			}
//		}));
//
//		for (final String terminologyId : allTerminologyIds) {
//			final Collection<IConfigurationElement> elements = getAllTerminologyLevelConfigurationElement(terminologyId, REFSET_MEMBERSHIP_LOOKUP_SERVICE_EXTENSION_POINT_ID);
//			for (final IConfigurationElement element : elements) {
//				searchers.add((IRefSetMembershipLookupService<String>) createExecutableExtension(element));
//			}
//		}
//
//		return searchers;
//	}

	public Collection<IValueSetMembershipLookupService> getValueSetMembershipLookupServices() {
		final Set<IValueSetMembershipLookupService> searchers = Sets.newHashSet();
		final IConfigurationElement[] elements = Platform.getExtensionRegistry().getConfigurationElementsFor(VALUE_SET_MEMBERSHIP_LOOKUP_SERVICE_EXTENSION_POINT_ID);
		for (final IConfigurationElement element : elements) {
			searchers.add((IValueSetMembershipLookupService) createExecutableExtension(element));
		}

		return searchers;
	}
	
	public Collection<IMappingSetMembershipLookupService> getMappingSetMembershipLookupServices() {
		final Set<IMappingSetMembershipLookupService> searchers = Sets.newHashSet();
		final IConfigurationElement[] elements = Platform.getExtensionRegistry().getConfigurationElementsFor(MAPPING_SET_MEMBERSHIP_LOOKUP_SERVICE_EXTENSION_POINT_ID);
		for (final IConfigurationElement element : elements) {
			searchers.add((IMappingSetMembershipLookupService) createExecutableExtension(element));
		}

		return searchers;
	}

	private static final IClientTerminologyBrowser<IComponent<String>, String> EMPTY_CLIENT_BROWSER = new ClientTerminologyBrowserAdapter<IComponent<String>, String>() {
		@Override public boolean isTerminologyAvailable() {
			return true;
		}
	};
	
	/**Returns with the {@link IClientTerminologyBrowserFactory terminology browser factory} for the given terminology component ID argument.
	 *<p>If there are no registered terminology browser factories associated with the given terminology component ID, this method returns with a factory 
	 *which creates an {@link ClientTerminologyBrowserAdapter empty terminology browser}. That empty terminology browser instance always returns with {@code true}
	 *for {@link IClientTerminologyBrowser#isTerminologyAvailable()} invocation.*/
	public IClientTerminologyBrowserFactory<String, IComponent<String>, IClientTerminologyBrowser<IComponent<String>, String>> getTerminologyBrowserFactory(
			final String terminologyComponentId) {
		
		final IConfigurationElement configurationElement = getTerminologyLevelConfigurationElementUnsafe(getTerminologyId(terminologyComponentId), TERMINOLOGY_BROWSER_FACTORY_EXTENSION_POINT_ID);
		if (null == configurationElement) {
			return new IClientTerminologyBrowserFactory<String, IComponent<String>, IClientTerminologyBrowser<IComponent<String>,String>>() {
				@Override public IClientTerminologyBrowser<IComponent<String>, String> getTerminologyBrowser() {
					return EMPTY_CLIENT_BROWSER;
				}
				@Override public IClientTerminologyBrowser<IComponent<String>, String> getTerminologyBrowser(IBranchPath branchPath) {
					return EMPTY_CLIENT_BROWSER;
				}
			};
		}
		return (IClientTerminologyBrowserFactory<String, IComponent<String>, IClientTerminologyBrowser<IComponent<String>, String>>) createExecutableExtension(configurationElement);
	}

	public INameProviderFactory getNameProviderFactory(final String terminologyComponentId) {
		return (INameProviderFactory) createExecutableExtension(getTerminologyComponentLevelConfigurationElement(terminologyComponentId, NAME_PROVIDER_SERVICE_EXTENSION_POINT_ID));
	}

	public <T, V> ILookupService<String, T, V> getLookupService(final String terminologyComponentId) {
		return (ILookupService<String, T, V>) createExecutableExtension(getTerminologyComponentLevelConfigurationElement(terminologyComponentId, LOOKUP_SERVICE_EXTENSION_POINT_ID));
	}

	/**
	 * Returns with the {@link IComponentIconIdProvider component icon ID provider} for a terminology component type given by its unique ID.
	 * @param terminologyComponentId the terminology component ID.
	 * @return the component icon ID provider instance.
	 */
	@SuppressWarnings("unchecked")
	public <K> IComponentIconIdProvider<K> getComponentIconIdProvider(final String terminologyComponentId) {
		Preconditions.checkNotNull(terminologyComponentId, "Terminology component ID argument cannot be null.");
		return (IComponentIconIdProvider<K>) createExecutableExtension(getTerminologyComponentLevelConfigurationElement(terminologyComponentId,
				COMPONENT_ICON_ID_PROVIDER_EXTENSION_POINT_ID));
	}

	/**Sugar for {@link #getComponentIconIdProvider(String)}.*/
	public <K> IComponentIconIdProvider<K> getComponentIconIdProvider(final short terminologyComponentId) {
		return getComponentIconIdProvider(getTerminologyComponentId(terminologyComponentId));
	}
	
	public Object createExecutableExtension(final IConfigurationElement configurationElement) {
		Preconditions.checkNotNull(configurationElement, "Configuration element argument cannot be null.");

		try {
			return configurationElement.createExecutableExtension(CLASS_ATTRIBUTE);
		} catch (final CoreException e) {
			throw new RuntimeException("Error while creating executable extension from the passed in configuration element: " + configurationElement);
		}
	}

	public Collection<String> getAllRegisteredTerminologyComponentsForTerminology(final String terminologyId) {
		return terminologyToComponentsSupplier.get().get(Preconditions.checkNotNull(terminologyId, "Terminology identifier argument cannot be null."));
	}
	
	public String getTerminologyIdForTerminologyComponentId(final String terminologyComponentId) {
		return componentToTerminologySupplier.get().get(Preconditions.checkNotNull(terminologyComponentId, "Terminology component ID argument cannot be null."));
	}
	
	public Collection<String> getAllRegisteredTerminologies() {
		return terminologyToComponentsSupplier.get().keySet();
	}
	
	public Multimap<String, String> getAllRegisteredTerminologiesWithComponents() {
		return terminologyToComponentsSupplier.get();
	}
	
	public Collection<IConfigurationElement> getAllTerminologyLevelConfigurationElement(final String terminologyId, final String extensionPointId) {
		Preconditions.checkNotNull(terminologyId, "Terminology identifier argument cannot be null.");
		Preconditions.checkNotNull(extensionPointId, "Extension point identifier name argument cannot be null.");
		Preconditions.checkArgument(!StringUtils.isEmpty(terminologyId), "Terminology identifier argument cannot be empty string.");
		Preconditions.checkArgument(!StringUtils.isEmpty(extensionPointId), "Extension point identifier argument cannot be empty string.");

		final Set<IConfigurationElement> elements = Sets.newHashSet();
		for (final IConfigurationElement element : Platform.getExtensionRegistry().getConfigurationElementsFor(extensionPointId)) {
			if (terminologyId.equals(element.getAttribute(TERMINOLOGY_ID_ATTRIBUTE))) {
				elements.add(element);
			}
		}
		return elements;
	}

	public IConfigurationElement getTerminologyLevelConfigurationElementUnsafe(final String terminologyId, final String extensionPointId) {
		Preconditions.checkNotNull(terminologyId, "Terminology identifier argument cannot be null.");
		Preconditions.checkNotNull(extensionPointId, "Extension point identifier name argument cannot be null.");
		Preconditions.checkArgument(!StringUtils.isEmpty(terminologyId), "Terminology identifier argument cannot be empty string.");
		Preconditions.checkArgument(!StringUtils.isEmpty(extensionPointId), "Extension point identifier argument cannot be empty string.");

		for (final IConfigurationElement element : Platform.getExtensionRegistry().getConfigurationElementsFor(extensionPointId)) {
			if (terminologyId.equals(element.getAttribute(TERMINOLOGY_ID_ATTRIBUTE))) {
				return element;
			}
		}
		return null;
	}
	
	public IConfigurationElement getTerminologyLevelConfigurationElement(final String terminologyId, final String extensionPointId) {
		Preconditions.checkNotNull(terminologyId, "Terminology identifier argument cannot be null.");
		Preconditions.checkNotNull(extensionPointId, "Extension point identifier name argument cannot be null.");
		Preconditions.checkArgument(!StringUtils.isEmpty(terminologyId), "Terminology identifier argument cannot be empty string.");
		Preconditions.checkArgument(!StringUtils.isEmpty(extensionPointId), "Extension point identifier argument cannot be empty string.");

		final IConfigurationElement configurationElement = getTerminologyLevelConfigurationElementUnsafe(terminologyId, extensionPointId);
		if (null == configurationElement) {
			throw new RuntimeException("No configuration element has been registered for '" + extensionPointId + "' extension with the '" + terminologyId
					+ "' terminology component identifier.");
		}
		
		return configurationElement;
	}

	public IConfigurationElement getTerminologyComponentLevelConfigurationElement(final String terminologyComponentId, final String extensionPointId) {
		Preconditions.checkNotNull(terminologyComponentId, "Terminology component identifier argument cannot be null.");
		Preconditions.checkNotNull(extensionPointId, "Extension point identifier name argument cannot be null.");
		Preconditions.checkArgument(!StringUtils.isEmpty(terminologyComponentId), "Terminology component identifier argument cannot be empty string.");
		Preconditions.checkArgument(!StringUtils.isEmpty(extensionPointId), "Extension point identifier name argument cannot be empty string.");

		for (final IConfigurationElement element : Platform.getExtensionRegistry().getConfigurationElementsFor(extensionPointId)) {
			final String attribute = element.getAttribute(TERMINOLOGY_COMPONENT_ID_ATTRIBUTE);
			if (terminologyComponentId.equals(attribute)) {
				return element;
			}
		}
		throw new RuntimeException("No configuration element has been registered for '" + extensionPointId + "' extension with the '" + terminologyComponentId
				+ "' terminology component identifier.");
	}
	
	/**Returns with the primary terminology component ID for the given terminology argument.*/
	public String getPrimaryComponentIdByTerminologyId(final String terminologyId) {
		Preconditions.checkNotNull(terminologyId, "Terminology ID argument cannot be null.");
		return primaryTerminologyComponentSupplier.get().get(terminologyId);
	}
	
	public String getPrimaryComponentIdByOid(final String oid) {
		return getTerminologyAttributeValueByOid(PRIMARY_COMPONENT_ID_ATTRIBUTE, oid);
	}

	public String getTerminologyIdByOid(final String oid) {
		return getTerminologyAttributeValueByOid(ID_ATTRIBUTE, oid);
	}

	public String getTerminologyId(final String terminologyComponentId) {
		Preconditions.checkNotNull(terminologyComponentId, "Terminology component identifier argument cannot be null.");
		Preconditions.checkArgument(!StringUtils.isEmpty(terminologyComponentId), "Terminology component identifier argument cannot be empty string.");

		for (final IConfigurationElement terminologComponents : Platform.getExtensionRegistry().getConfigurationElementsFor(TERMINOLOGY_COMPONENT_EXTENSION_POINT_ID)) {
			if (terminologyComponentId.equals(terminologComponents.getAttribute(ID_ATTRIBUTE))) {
				return terminologComponents.getAttribute(TERMINOLOGY_ID_ATTRIBUTE);
			}
		}
		throw new IllegalArgumentException("No terminology extension has been registered for the passed in terminology component identifier: " + terminologyComponentId);
	}

	public boolean isEffectiveTimeSupported(final String terminologyId) {
		Preconditions.checkNotNull(terminologyId, "terminologyId");
		//iterate through all registered terminology component extensions
		for (final IConfigurationElement terminology : Platform.getExtensionRegistry().getConfigurationElementsFor(TERMINOLOGY_EXTENSION_POINT_ID)) {
			if (terminologyId.equals(terminology.getAttribute(ID_ATTRIBUTE))) {
				final String supportsEffectiveTime = nullToEmpty(terminology.getAttribute(SUPPORTS_EFFECTIVE_TIME_ATTRIBUTE));
				return Boolean.parseBoolean(supportsEffectiveTime);
			}
		}
		throw new RuntimeException("Cannot find terminology with ID: '" + terminologyId + "'.");
	}
	
	public ICoreTerminologyComponentInformation getComponentInformation(final Object object) {
		Preconditions.checkNotNull(object, "Object argument cannot be null.");
		return getComponentInformation(CoreTerminologyBroker.getInstance().getTerminologyComponentId(object));
	}

	public ICoreTerminologyInformation getTerminologyInformation(final String terminologyId) {
		if (StringUtils.isEmpty(terminologyId)) {
			return NOOP_TERMINOLOGY_INFORMATION;
		}
		return Preconditions.checkNotNull(registeredTerminologiesSupplier.get().get(terminologyId), "Cannot find registered terminology for ID: " + terminologyId);
	}
	
	/**Returns with the human readable name of the corresponding tooling feature for the given terminology ID argument.*/
	public String getTerminologyName(final String terminologyId) {
		ICoreTerminologyInformation information = registeredTerminologiesSupplier.get().get(terminologyId);
		return null == information ? terminologyId : information.getName();
	}
	
	private static final ICoreTerminologyInformation NOOP_TERMINOLOGY_INFORMATION = new ICoreTerminologyInformation() {
		@Override public String getName() { return ""; }
		@Override public String getId() { return ""; }
	};
	
	public ICoreTerminologyComponentInformation getComponentInformation(final short terminologyComponentId) {
		return getComponentInformation(getTerminologyComponentId(terminologyComponentId));
	}
	
	public ICoreTerminologyComponentInformation getComponentInformation(final String terminologyComponentId) {
		Preconditions.checkNotNull(terminologyComponentId, "Terminology component identifier argument cannot be null.");
		return UNSPECIFIED.equals(terminologyComponentId) ? UNSPECIFIED_COMPONENT : internalGetRegisteredComponents().get(terminologyComponentId);
	}

	private String getTerminologyAttributeValueByOid(final String attribute, final String oid) {
		Preconditions.checkArgument(!StringUtils.isEmpty(attribute), "Terminology extension attribute argument cannot be empty.");
		Preconditions.checkNotNull(oid, "Terminology OID argument cannot be null.");
		Preconditions.checkArgument(!StringUtils.isEmpty(oid), "Terminology OID argument cannot be empty.");

		for (final IConfigurationElement terminologComponents : Platform.getExtensionRegistry().getConfigurationElementsFor(TERMINOLOGY_EXTENSION_POINT_ID)) {
			if (oid.equals(terminologComponents.getAttribute(OID_ATTRIBUTE))) {
				return terminologComponents.getAttribute(attribute);
			}
		}
		throw new IllegalArgumentException("No terminology extension has been registered for the passed in terminology OID: " + oid);
	}

	private Map<String, ICoreTerminologyComponentInformation> internalGetRegisteredComponents() {

		if (null == registeredTerminologyComponents) {

			synchronized (CoreTerminologyBroker.class) {

				if (null == registeredTerminologyComponents) {

					final IConfigurationElement[] terminologyComponents = Platform.getExtensionRegistry().getConfigurationElementsFor(TERMINOLOGY_COMPONENT_EXTENSION_POINT_ID);
					final List<ICoreTerminologyComponentInformation> terminologyComponentInfos = configurationElementsToComponentInfo(Lists.newArrayList(terminologyComponents));
					registeredTerminologyComponents = Maps.uniqueIndex(terminologyComponentInfos, new Function<ICoreTerminologyComponentInformation, String>() {
						@Override
						public String apply(final ICoreTerminologyComponentInformation info) {
							return info.getId();
						}
					});
				}

			}

		}

		return registeredTerminologyComponents;
	}

	private List<ICoreTerminologyComponentInformation> configurationElementsToComponentInfo(final List<IConfigurationElement> elements) {
		return Lists.transform(elements,
				new Function<IConfigurationElement, ICoreTerminologyComponentInformation>() {

					//implements com.google.common.base.Function<org.eclipse.core.runtime.IConfigurationElement,com.b2international.snowowl.ui.broker.WorkbenchTerminologyBroker.ITerminologyComponentInformation>.apply
					@Override
					public ICoreTerminologyComponentInformation apply(final IConfigurationElement element) {
						return new ICoreTerminologyComponentInformation() {
							@Override
							public String getName() {
								return element.getAttribute(NAME_ATTRIBUTE);
							}

							@Override
							public String getId() {
								return element.getAttribute(CoreTerminologyBroker.ID_ATTRIBUTE);
							}
						};
					}

				});
	}

	public ISearchResultProvider<String, IComponent<String>> getSearchResultProvider(final String terminologyComponentId) {
		return (ISearchResultProvider<String, IComponent<String>>) createExecutableExtension(getTerminologyComponentLevelConfigurationElement(terminologyComponentId,
				SEARCH_RESULT_PROVIDER_EXTENSION_POINT_ID));
	}
	
}