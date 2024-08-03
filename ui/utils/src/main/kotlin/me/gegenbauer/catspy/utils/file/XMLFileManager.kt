package me.gegenbauer.catspy.utils.file

import org.w3c.dom.Document
import org.w3c.dom.Element
import org.w3c.dom.Node
import org.w3c.dom.NodeList
import org.xml.sax.InputSource
import java.io.StringReader
import java.io.StringWriter
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.OutputKeys
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult

class XMLFileManager : StringFileManager() {

    override val fileExtension: String = FILE_EXTENSION

    fun parseXMLString(xmlString: String): Map<String, Any> {
        val resultMap = HashMap<String, Any>()
        if (xmlString.isEmpty()) return resultMap

        // Create a DocumentBuilderFactory
        val factory = DocumentBuilderFactory.newInstance()
        factory.isNamespaceAware = true

        // Create a DocumentBuilder
        val builder = factory.newDocumentBuilder()

        // Parse the XML string into a Document
        val document: Document = builder.parse(InputSource(StringReader(xmlString)))

        // Normalize the XML structure
        document.documentElement.normalize()

        // Get the root element
        val root: Element = document.documentElement

        // Traverse the document and extract elements into the map
        val nodeList: NodeList = root.childNodes
        for (i in 0 until nodeList.length) {
            val node: Node = nodeList.item(i)
            if (node.nodeType == Node.ELEMENT_NODE) {
                val element = node as Element
                val tagName = element.tagName
                val textContent = element.textContent

                if (resultMap.containsKey(tagName)) {
                    val existingValue = resultMap[tagName]
                    if (existingValue is String) {
                        val list = ArrayList<String>()
                        list.add(existingValue)
                        list.add(textContent)
                        resultMap[tagName] = list
                    } else if (existingValue is ArrayList<*>) {
                        (existingValue as MutableList<String>).add(textContent)
                    }
                } else {
                    resultMap[tagName] = textContent
                }
            }
        }

        return resultMap
    }

    fun mapToXMLString(map: Map<String, Any>): String {
        val factory = DocumentBuilderFactory.newInstance()
        val builder = factory.newDocumentBuilder()
        val document: Document = builder.newDocument()

        // Create root element
        val root: Element = document.createElement("root")
        document.appendChild(root)

        // Traverse the map and add elements to the document
        map.forEach { (key, value) ->
            if (value is List<*>) {
                value.forEach { item ->
                    val element = document.createElement(key)
                    element.appendChild(document.createTextNode(item.toString()))
                    root.appendChild(element)
                }
            } else {
                val element = document.createElement(key)
                element.appendChild(document.createTextNode(value.toString()))
                root.appendChild(element)
            }
        }

        // Convert the document to a string
        val transformerFactory = TransformerFactory.newInstance()
        val transformer = transformerFactory.newTransformer()
        transformer.setOutputProperty(OutputKeys.INDENT, "yes")
        val source = DOMSource(document)
        val result = StringWriter()
        val streamResult = StreamResult(result)
        transformer.transform(source, streamResult)

        return result.toString()
    }

    companion object {
        const val FILE_EXTENSION = "xml"
    }
}