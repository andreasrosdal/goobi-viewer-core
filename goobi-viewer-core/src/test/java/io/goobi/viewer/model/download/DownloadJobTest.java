/**
 * This file is part of the Goobi viewer - a content presentation and management application for digitized objects.
 *
 * Visit these websites for more information.
 *          - http://www.intranda.com
 *          - http://digiverso.com
 *
 * This program is free software; you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package io.goobi.viewer.model.download;

import static org.junit.Assert.assertEquals;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import io.goobi.viewer.AbstractDatabaseAndSolrEnabledTest;
import io.goobi.viewer.exceptions.DownloadException;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.model.job.download.DownloadJob;
import io.goobi.viewer.model.job.download.PDFDownloadJob;

public class DownloadJobTest extends AbstractDatabaseAndSolrEnabledTest {

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
    }

    /**
     * @see DownloadJob#isExpired()
     * @verifies return correct value
     */
    @Test
    public void isExpired_shouldReturnCorrectValue() throws Exception {
        DownloadJob job = new PDFDownloadJob("PI_3", null, LocalDateTime.now(), 0);
        Thread.sleep(5);
        Assert.assertTrue(job.isExpired());
        job.setTtl(30000);
        Assert.assertFalse(job.isExpired());
    }

    /**
     * @see DownloadJob#generateDownloadJobId(String[])
     * @verifies generate same id from same criteria
     */
    @Test
    public void generateDownloadJobId_shouldGenerateSameIdFromSameCriteria() throws Exception {
        String hash = "07319d093ea0e44a618cdf3accb9576009025f7ea7ed3b6765192f1ddca6a801";
        String crit1 = "PPN123456789";
        String crit2 = "LOG_0000";
        Assert.assertEquals(hash, DownloadJob.generateDownloadJobId(crit1, crit2));
        Assert.assertEquals(hash, DownloadJob.generateDownloadJobId(crit1, crit2));
        Assert.assertEquals(hash, DownloadJob.generateDownloadJobId(crit1, crit2));
    }

    //    @Test
    public void testSendJobToTaskManager() throws PresentationException, IndexUnreachableException {
        boolean triggered = false;
        try {
            PDFDownloadJob.triggerCreation("18979459_1830", "LOG_0003", "6c685d274f44f6e3ab8ef3f1c640bd01");
            triggered = true;
        } catch (DownloadException e) {

        }
        Assert.assertTrue("TaskManager job not triggered", triggered);
    }

    //    @Test
    public void testTaskManagerQueue() throws InterruptedException, PresentationException, IndexUnreachableException {
        List<String> logs = new ArrayList<>();
        logs.add("");
        logs.add("LOG_0004");
        logs.add("LOG_0005");
        logs.add("LOG_0006");
        logs.add("LOG_0007");
        logs.add("LOG_0008");
        String pi = "18979459_1830";
        for (String logId : logs) {
            PDFDownloadJob.triggerCreation(pi, logId, DownloadJob.generateDownloadJobId(pi, logId));
        }
        Thread.sleep(500);
        for (String logId : logs) {
            String id = DownloadJob.generateDownloadJobId(pi, logId);
            int jobsUntil = PDFDownloadJob.getPDFJobsInQueue(id);
            assertEquals(4, jobsUntil, 3);
        }
    }

    @Test 
    public void testPutDownloadJobAnswer() throws JsonProcessingException {
        String pi = "18979459_1830";
        String logid = "LOG_0004";
        DownloadJob job = new PDFDownloadJob(pi, logid, LocalDateTime.now(), 1000);
        job.setMessage("Some message");
        job.getObservers().add("me@he.re");
        String jobString = new ObjectMapper().registerModule(new JavaTimeModule()).writeValueAsString(job);
    }
}
