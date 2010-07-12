package com.aptana.editor.js.contentassist.index;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.aptana.editor.js.Activator;
import com.aptana.editor.js.contentassist.model.FieldSelector;
import com.aptana.editor.js.contentassist.model.FunctionElement;
import com.aptana.editor.js.contentassist.model.ParameterElement;
import com.aptana.editor.js.contentassist.model.PropertyElement;
import com.aptana.editor.js.contentassist.model.ReturnTypeElement;
import com.aptana.editor.js.contentassist.model.TypeElement;
import com.aptana.editor.js.contentassist.model.UserAgentElement;
import com.aptana.index.core.Index;
import com.aptana.index.core.QueryResult;
import com.aptana.index.core.SearchPattern;

public class JSIndexReader
{
	/**
	 * createFunctionFromKey
	 * 
	 * @param index
	 * @param key
	 * @param fields
	 * @return
	 * @throws IOException
	 */
	protected FunctionElement createFunction(Index index, QueryResult function, EnumSet<FieldSelector> fields) throws IOException
	{
		FunctionElement f = new FunctionElement();

		if (fields.isEmpty() == false)
		{
			String key = function.getWord();
			String[] columns = key.split(JSIndexConstants.DELIMITER);
			int column = 0;
			
			// name
			if (fields.contains(FieldSelector.NAME))
			{
				f.setName(columns[column]);
			}
			column++;
			
			// owning type
			column++;	// skip owning type
			
			// description
			if (fields.contains(FieldSelector.DESCRIPTION))
			{
				f.setDescription(this.getDescription(index, columns[column]));
			}
			column++;
			
			// parameters
			if (fields.contains(FieldSelector.PARAMETERS))
			{
				for (ParameterElement parameter : this.getParameters(index, columns[column]))
				{
					f.addParameter(parameter);
				}
			}
			column++;
	
			// return types
			if (fields.contains(FieldSelector.RETURN_TYPES))
			{
				for (ReturnTypeElement returnType : this.getReturnTypes(index, columns[column]))
				{
					f.addReturnType(returnType);
				}
			}
			column++;
			
			if (column < columns.length)
			{
				// user agents
				if (fields.contains(FieldSelector.USER_AGENTS))
				{
					for (String userAgentKey : columns[column].split(JSIndexConstants.SUB_DELIMITER))
					{
						// get user agent and add to element
						f.addUserAgent(this.getUserAgent(index, userAgentKey));
					}
				}
				column++;
			}
			
			// documents
			if (fields.contains(FieldSelector.DOCUMENTS))
			{
				for (String document : function.getDocuments())
				{
					f.addDocument(document);
				}
			}
		}
		
		return f;
	}
	
	/**
	 * createProperty
	 * 
	 * @param index
	 * @param key
	 * @param fields
	 * @return
	 * @throws IOException 
	 */
	protected PropertyElement createProperty(Index index, QueryResult property, EnumSet<FieldSelector> fields) throws IOException
	{
		PropertyElement p = new PropertyElement();

		if (fields.isEmpty() == false)
		{
			String key = property.getWord();
			String[] columns = key.split(JSIndexConstants.DELIMITER);
			int column = 0;
			
			// name
			if (fields.contains(FieldSelector.NAME))
			{
				p.setName(columns[column]);
			}
			column++;
			
			// owning type
			column++;
			
			// description
			if (fields.contains(FieldSelector.DESCRIPTION))
			{
				p.setDescription(this.getDescription(index, columns[column]));
			}
			column++;
			
			// types
			if (fields.contains(FieldSelector.TYPES))
			{
				for (ReturnTypeElement returnType : this.getReturnTypes(index, columns[column]))
				{
					p.addType(returnType);
				}
			}
			column++;
			
			if (column < columns.length)
			{
				if (fields.contains(FieldSelector.USER_AGENTS))
				{
					// user agents
					for (String userAgentKey : columns[column].split(JSIndexConstants.SUB_DELIMITER))
					{
						// get user agent and add to element
						p.addUserAgent(this.getUserAgent(index, userAgentKey));
					}
					column++;
				}
			}
			
			// documents
			if (fields.contains(FieldSelector.DOCUMENTS))
			{
				for (String document : property.getDocuments())
				{
					p.addDocument(document);
				}
			}
		}
		
		return p;
	}
	
	/**
	 * getDescription
	 * 
	 * @param index
	 * @param descriptionKey
	 * @return
	 * @throws IOException
	 */
	protected String getDescription(Index index, String descriptionKey) throws IOException
	{
		String result = ""; //$NON-NLS-1$

		if (index != null && descriptionKey != null && descriptionKey.length() > 0 && !descriptionKey.equals(JSIndexConstants.NO_ENTRY))
		{
			// grab description
			String descriptionPattern = descriptionKey + JSIndexConstants.DELIMITER;
			List<QueryResult> descriptions = index.query(new String[] { JSIndexConstants.DESCRIPTION }, descriptionPattern, SearchPattern.PREFIX_MATCH);
	
			if (descriptions != null)
			{
				String descriptionValue = descriptions.get(0).getWord();
	
				result = descriptionValue.substring(descriptionValue.indexOf(JSIndexConstants.DELIMITER) + 1);
			}
		}

		return result;
	}
	
	/**
	 * getFunction
	 * 
	 * @param index
	 * @param owningType
	 * @param propertyName
	 * @param fields
	 * @return
	 * @throws IOException
	 */
	public FunctionElement getFunction(Index index, String owningType, String propertyName, EnumSet<FieldSelector> fields) throws IOException
	{
		FunctionElement result = null;
		
		if (index != null)
		{
			List<QueryResult> functions = index.query(new String[] { JSIndexConstants.FUNCTION }, this.getMemberPattern(owningType, propertyName), SearchPattern.REGEX_MATCH);
			
			if (functions != null && functions.size() > 0)
			{
				result = this.createFunction(index, functions.get(0), fields);
			}
		}
		
		return result;
	}
	
	/**
	 * getFunctions
	 * 
	 * @param index
	 * @param owningType
	 * @param fields
	 * @return
	 * @throws IOException
	 */
	public List<FunctionElement> getFunctions(Index index, String owningType, EnumSet<FieldSelector> fields) throws IOException
	{
		List<FunctionElement> result = new LinkedList<FunctionElement>();
		
		if (index != null)
		{
			// read properties
			List<QueryResult> functions = index.query(new String[] { JSIndexConstants.FUNCTION }, this.getMemberPattern(owningType), SearchPattern.REGEX_MATCH);
	
			if (functions != null)
			{
				for (QueryResult function : functions)
				{
					FunctionElement f = this.createFunction(index, function, fields);
					
					result.add(f);
				}
			}
		}

		return result;
	}

	/**
	 * getMemberPattern
	 * 
	 * @param typeName
	 * @return
	 */
	private String getMemberPattern(String typeName)
	{
		return MessageFormat.format("^[^{0}]+{0}{1}(?:{0}|$)", new Object[] { JSIndexConstants.DELIMITER, typeName }); //$NON-NLS-1$
	}
	
	/**
	 * getMemberPattern
	 * 
	 * @param typeName
	 * @param memberName
	 * @return
	 */
	private String getMemberPattern(String typeName, String memberName)
	{
		return MessageFormat.format("^{2}{0}{1}(?:{0}|$)", new Object[] { JSIndexConstants.DELIMITER, typeName, memberName }); //$NON-NLS-1$
	}

	/**
	 * getParameters
	 * 
	 * @param index
	 * @param parametersKey
	 * @return
	 * @throws IOException
	 */
	protected List<ParameterElement> getParameters(Index index, String parametersKey) throws IOException
	{
		List<ParameterElement> result = new LinkedList<ParameterElement>();
		
		if (index != null)
		{
			String descriptionPattern = parametersKey + JSIndexConstants.DELIMITER;
			List<QueryResult> parameters = index.query(new String[] { JSIndexConstants.PARAMETERS }, descriptionPattern, SearchPattern.PREFIX_MATCH);
	
			if (parameters != null && parameters.size() > 0)
			{
				String parametersValue = parameters.get(0).getWord();
				String[] parameterValues = parametersValue.split(JSIndexConstants.DELIMITER);
	
				for (int i = 1; i < parameterValues.length; i++)
				{
					String parameterValue = parameterValues[i];
					String[] columns = parameterValue.split(","); //$NON-NLS-1$
					ParameterElement parameter = new ParameterElement();
	
					parameter.setName(columns[0]);
					parameter.setUsage(columns[1]);
	
					for (int j = 2; j < columns.length; j++)
					{
						parameter.addType(columns[j]);
					}
					
					result.add(parameter);
				}
			}
		}

		return result;
	}
	
	/**
	 * getProperties
	 * 
	 * @param index
	 * @param owningType
	 * @param fields
	 * @return
	 * @throws IOException
	 */
	public List<PropertyElement> getProperties(Index index, String owningType, EnumSet<FieldSelector> fields) throws IOException
	{
		List<PropertyElement> result = new LinkedList<PropertyElement>();
		
		if (index != null)
		{
			// read properties
			List<QueryResult> properties = index.query(new String[] { JSIndexConstants.PROPERTY }, this.getMemberPattern(owningType), SearchPattern.REGEX_MATCH);
	
			if (properties != null)
			{
				for (QueryResult property : properties)
				{
					PropertyElement p = this.createProperty(index, property, fields);
					
					result.add(p);
				}
			}
		}

		return result;
	}

	/**
	 * getProperty
	 * 
	 * @param index
	 * @param owningType
	 * @param propertyName
	 * @param fields
	 * @return
	 * @throws IOException
	 */
	public PropertyElement getProperty(Index index, String owningType, String propertyName, EnumSet<FieldSelector> fields) throws IOException
	{
		PropertyElement result = null;
		
		if (index != null)
		{
			List<QueryResult> properties = index.query(new String[] { JSIndexConstants.PROPERTY }, this.getMemberPattern(owningType, propertyName), SearchPattern.REGEX_MATCH);
			
			if (properties != null && properties.size() > 0)
			{
				result = this.createProperty(index, properties.get(0), fields);
			}
		}
		
		return result;
	}

	/**
	 * getReturnTypes
	 * 
	 * @param index
	 * @param returnTypesKey
	 * @return
	 * @throws IOException 
	 */
	protected List<ReturnTypeElement> getReturnTypes(Index index, String returnTypesKey) throws IOException
	{
		List<ReturnTypeElement> result = new LinkedList<ReturnTypeElement>();
		
		if (index != null)
		{
			String descriptionPattern = returnTypesKey + JSIndexConstants.DELIMITER;
			List<QueryResult> returnTypes = index.query(new String[] { JSIndexConstants.RETURN_TYPES }, descriptionPattern, SearchPattern.PREFIX_MATCH);
	
			if (returnTypes != null && returnTypes.size() > 0)
			{
				String word = returnTypes.get(0).getWord();
				String[] returnTypesValues = word.split(JSIndexConstants.DELIMITER);
	
				for (int i = 1; i < returnTypesValues.length; i++)
				{
					String returnTypeValue = returnTypesValues[i];
					String[] columns = returnTypeValue.split(","); //$NON-NLS-1$
					ReturnTypeElement returnType = new ReturnTypeElement();
	
					returnType.setType(columns[0]);
					returnType.setDescription(this.getDescription(index, columns[1]));
	
					result.add(returnType);
				}
			}
		}

		return result;
	}
	
	/**
	 * getType
	 * 
	 * @param index
	 * @param typeName
	 * @return
	 */
	public TypeElement getType(Index index, String typeName, EnumSet<FieldSelector> fields)
	{
		TypeElement result = null;

		if (index != null)
		{
			try
			{
				String pattern = typeName + JSIndexConstants.DELIMITER;
				List<QueryResult> types = index.query(new String[] { JSIndexConstants.TYPE }, pattern, SearchPattern.PREFIX_MATCH);
	
				if (types != null && types.size() > 0)
				{
					QueryResult type = types.get(0);
					String[] columns = type.getWord().split(JSIndexConstants.DELIMITER);
					String retrievedName = columns[0];
					int column = 0;
	
					// create type
					result = new TypeElement();
					
					if (fields.isEmpty() == false)
					{
						// name
						if (fields.contains(FieldSelector.NAME))
						{
							result.setName(columns[column]);
						}
						column++;
						
						// super types
						if (fields.contains(FieldSelector.PARENT_TYPES))
						{
							for (String parentType : columns[column].split(JSIndexConstants.SUB_DELIMITER))
							{
								result.addParentType(parentType);
							}
						}
						column++;
						
						// description
						if (fields.contains(FieldSelector.DESCRIPTION))
						{
							result.setDescription(this.getDescription(index, columns[column]));
						}
						column++;
		
						// properties
						if (fields.contains(FieldSelector.PROPERTIES))
						{
							for (PropertyElement property : this.getProperties(index, retrievedName, EnumSet.allOf(FieldSelector.class)))
							{
								result.addProperty(property);
							}
						}
		
						// functions
						if (fields.contains(FieldSelector.FUNCTIONS))
						{
							for (FunctionElement function: this.getFunctions(index, retrievedName, EnumSet.allOf(FieldSelector.class)))
							{
								result.addProperty(function);
							}
						}
						
						// documents
						if (fields.contains(FieldSelector.DOCUMENTS))
						{
							for (String document : type.getDocuments())
							{
								result.addDocument(document);
							}
						}
					}
				}
			}
			catch (IOException e)
			{
			}
		}

		return result;
	}

	/**
	 * getTypeProperties
	 * 
	 * @param index
	 * @param typeName
	 * @param fields
	 * @return
	 * @throws IOException 
	 */
	public List<PropertyElement> getTypeProperties(Index index, String typeName, EnumSet<FieldSelector> fields) throws IOException
	{
		List<PropertyElement> properties = this.getProperties(index, typeName, fields);
		
		properties.addAll(this.getFunctions(index, typeName, fields));
		
		return properties;
	}
	
	/**
	 * getUserAgent
	 * 
	 * @param index
	 * @param userAgentKey
	 * @return
	 * @throws IOException
	 */
	protected UserAgentElement getUserAgent(Index index, String userAgentKey) throws IOException
	{
		UserAgentElement result = JSIndexWriter.userAgentsByKey.get(userAgentKey);
		
		if (result == null && index != null)
		{
			String searchKey = userAgentKey + JSIndexConstants.DELIMITER;
			List<QueryResult> items = index.query(new String[] { JSIndexConstants.USER_AGENT }, searchKey,
					SearchPattern.PREFIX_MATCH);
	
			if (items != null && items.size() > 0)
			{
				String key = items.get(0).getWord();
				String[] columns = key.split(JSIndexConstants.DELIMITER);
				int column = 1; // skip index
	
				result = new UserAgentElement();
				result.setDescription(columns[column++]);
				result.setOS(columns[column++]);
				result.setPlatform(columns[column++]);
	
				// NOTE: split does not return a final empty element if the string being split
				// ends with the delimiter.
				if (column < columns.length)
				{
					result.setVersion(columns[column++]);
				}
			}
		}

		return result;
	}
	
	/**
	 * getValues
	 * 
	 * @return
	 */
	public Map<String, List<String>> getValues(Index index, String category)
	{
		Map<String, List<String>> result = null;

		if (index != null)
		{
			String pattern = "*"; //$NON-NLS-1$

			try
			{
				List<QueryResult> items = index.query(new String[] { category }, pattern, SearchPattern.PATTERN_MATCH);

				if (items != null && items.size() > 0)
				{
					result = new HashMap<String, List<String>>();

					for (QueryResult item : items)
					{
						result.put(item.getWord(), Arrays.asList(item.getDocuments()));
					}
				}
			}
			catch (IOException e)
			{
				Activator.logError(e.getMessage(), e);
			}
		}

		return result;
	}
}
