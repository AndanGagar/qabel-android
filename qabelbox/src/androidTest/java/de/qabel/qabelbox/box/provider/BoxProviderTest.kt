package de.qabel.qabelbox.box.provider

import android.content.Context
import android.provider.DocumentsContract
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.should.shouldMatch
import de.qabel.qabelbox.BuildConfig
import de.qabel.qabelbox.box.dto.*
import de.qabel.qabelbox.box.interactor.ProviderUseCase
import de.qabel.qabelbox.stubResult
import de.qabel.qabelbox.util.asString
import de.qabel.qabelbox.util.toByteArrayInputStream
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.mockito.Mockito
import rx.lang.kotlin.toSingletonObservable
import java.io.File
import java.io.FileOutputStream
import java.util.*

class BoxProviderTest : MockedBoxProviderTest() {
    override val context: Context
        get() = instrumentation.targetContext

    lateinit var testFileName: String
    lateinit var useCase: ProviderUseCase

    val docId = DocumentId("identity", "prefix", BoxPath.Root)
    val volume = VolumeRoot("root", docId.toString(), "alias")
    val volumes = listOf(volume)
    val sample = BrowserEntry.File("foobar.txt", 42000, Date())
    val sampleFiles = listOf(sample)
    val samplePayLoad = "foobar"

    override fun setUp() {
        super.setUp()
        useCase = Mockito.mock(ProviderUseCase::class.java)
        provider.injectProvider(useCase)

        val tmpDir = File(System.getProperty("java.io.tmpdir"))
        val file = File.createTempFile("testfile", "test", tmpDir)
        val outputStream = FileOutputStream(file)
        val testData = ByteArray(1024)
        Arrays.fill(testData, 'f'.toByte())
        for (i in 0..99) {
            outputStream.write(testData)
        }
        outputStream.close()
        testFileName = file.absolutePath
    }

    fun testQueryRoots() {
        stubResult(useCase.availableRoots(), volumes)
        val cursor = provider.queryRoots(BoxProvider.DEFAULT_ROOT_PROJECTION)
        assertThat(cursor.count, `is`(1))
        cursor.moveToFirst()
        val documentId = cursor.getString(6)
        documentId shouldMatch equalTo(volume.documentID)
    }

    fun testQueryDocument() {
        val document = docId.copy(path = BoxPath.Root * "foobar.txt")
        stubResult(useCase.query(document), sample.toSingletonObservable())
        val query = provider.queryDocument(document.toString(), null)
                ?: throw AssertionError("cursor null")
        query.moveToFirst()
        val idCol = query.getColumnIndex(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
        query.getString(idCol) shouldMatch equalTo(document.toString())
    }

    fun testQueryChildDocuments() {
        val document = docId.copy(path = BoxPath.Root)
        val sampleId = document.copy(path = BoxPath.Root * sample.name)
        val sampleFolder = document.copy(path = BoxPath.Root / "folder")
        val listing = listOf(ProviderEntry(sampleId, sample),
                             ProviderEntry(sampleFolder, BrowserEntry.Folder("folder")))
        stubResult(useCase.queryChildDocuments(document), listing.toSingletonObservable())

        val query = provider.queryChildDocuments(document.toString(), null, null)

        query.count shouldMatch equalTo(2)
        query.moveToFirst()
        val idCol = query.getColumnIndex(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
        query.getString(idCol) shouldMatch equalTo(sampleId.toString())
        query.moveToNext()
        query.getString(idCol) shouldMatch equalTo(sampleFolder.toString())
    }

    fun testOpenDocument() {
        val document = docId.copy(path = BoxPath.Root * "foobar.txt")
        stubResult(useCase.query(document), sample.toSingletonObservable())
        stubResult(useCase.download(document),
                ProviderDownload(document,
                    DownloadSource(sample,
                    samplePayLoad.toByteArrayInputStream())).toSingletonObservable())
        val documentUri = DocumentsContract.buildDocumentUri(
                BuildConfig.APPLICATION_ID + BoxProvider.AUTHORITY, document.toString())
        mockContentResolver.query(documentUri, null, null, null, null).use {
            assertTrue("No result for query", it.moveToFirst())
            val inputStream = mockContentResolver.openInputStream(documentUri)
            inputStream.asString() shouldMatch equalTo(samplePayLoad)
        }
    }

    /*
    @Ignore("Files stuff rewrite")
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    fun testCreateFile() {
        val testDocId = ROOT_DOC_ID + "testfile.png"
        val parentDocumentUri = DocumentsContract.buildDocumentUri(BuildConfig.APPLICATION_ID + BoxProvider.AUTHORITY, ROOT_DOC_ID)
        val documentUri = DocumentsContract.buildDocumentUri(BuildConfig.APPLICATION_ID + BoxProvider.AUTHORITY, testDocId)
        Assert.assertNotNull("Could not build document URI", documentUri)
        var query: Cursor = mockContentResolver.query(documentUri, null, null, null, null)
        Assert.assertNull("Document already there: " + documentUri.toString(), query)
        val document = DocumentsContract.createDocument(mockContentResolver, parentDocumentUri,
                "image/png",
                "testfile.png")
        Assert.assertNotNull("Create document failed, no document Uri returned", document)
        assertThat(document.toString(), `is`(documentUri.toString()))
        query = mockContentResolver.query(documentUri, null, null, null, null)
        Assert.assertNotNull("Document not created:" + documentUri.toString(), query)
    }

    @Ignore("Files stuff rewrite")
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    fun testDeleteFile() {
        val testDocId = ROOT_DOC_ID + "testfile.png"
        val parentDocumentUri = DocumentsContract.buildDocumentUri(BuildConfig.APPLICATION_ID + BoxProvider.AUTHORITY, ROOT_DOC_ID)
        val documentUri = DocumentsContract.buildDocumentUri(BuildConfig.APPLICATION_ID + BoxProvider.AUTHORITY, testDocId)
        Assert.assertNotNull("Could not build document URI", documentUri)
        var query: Cursor = mockContentResolver.query(documentUri, null, null, null, null)
        Assert.assertNull("Document already there: " + documentUri.toString(), query)
        val document = DocumentsContract.createDocument(mockContentResolver, parentDocumentUri,
                "image/png",
                "testfile.png")
        Assert.assertNotNull(document)
        DocumentsContract.deleteDocument(mockContentResolver, document)
        assertThat(document.toString(), `is`(documentUri.toString()))
        query = mockContentResolver.query(documentUri, null, null, null, null)
        Assert.assertNull("Document not deleted:" + documentUri.toString(), query)
    }

    @Ignore("Files stuff rewrite")
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    fun testRenameFile() {
        val testDocId = ROOT_DOC_ID + "testfile.png"
        val parentDocumentUri = DocumentsContract.buildDocumentUri(BuildConfig.APPLICATION_ID + BoxProvider.AUTHORITY, ROOT_DOC_ID)
        val documentUri = DocumentsContract.buildDocumentUri(BuildConfig.APPLICATION_ID + BoxProvider.AUTHORITY, testDocId)
        Assert.assertNotNull("Could not build document URI", documentUri)
        var query: Cursor = mockContentResolver.query(documentUri, null, null, null, null)
        Assert.assertNull("Document already there: " + documentUri.toString(), query)
        val document = DocumentsContract.createDocument(
                mockContentResolver, parentDocumentUri,
                "image/png",
                "testfile.png")
        Assert.assertNotNull(document)
        val renamed = DocumentsContract.renameDocument(mockContentResolver,
                document, "testfile2.png")
        Assert.assertNotNull(renamed)
        assertThat(renamed.toString(), `is`(parentDocumentUri.toString() + "testfile2.png"))
        query = mockContentResolver.query(documentUri, null, null, null, null)
        Assert.assertNull("Document not renamed:" + documentUri.toString(), query)
        query = mockContentResolver.query(renamed, null, null, null, null)
        Assert.assertNotNull("Document not renamed:" + documentUri.toString(), query)
    }

    /*
    @Throws(QblStorageException::class)
    fun testGetDocumentId() {
        assertThat(volume.getDocumentId("/"), `is`(MockedBoxProviderTest.ROOT_DOC_ID))
        val navigate = volume.navigate()
        assertThat(volume.getDocumentId(navigate.path), `is`(MockedBoxProviderTest.ROOT_DOC_ID))
        val folder = navigate.createFolder("testfolder")
        assertThat(navigate.getPath(folder), `is`("/testfolder/"))
        navigate.commit()
        navigate.navigate(folder)
        assertThat(volume.getDocumentId(navigate.path), `is`(MockedBoxProviderTest.ROOT_DOC_ID + "testfolder/"))
    }
    */
    * */

    companion object {
        private val TAG = "BoxProviderTest"
    }

}

