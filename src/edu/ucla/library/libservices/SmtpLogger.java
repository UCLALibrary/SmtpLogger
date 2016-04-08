/*Copyright © 2003-2007 The Regents of the University of California.
All Rights Reserved.

Permission to use, copy, modify, and distribute this software and its 
documentation for educational, research and non-profit purposes, without fee, 
and without a written agreement is hereby granted, provided that the above 
copyright notice, this paragraph and the following three paragraphs appear in 
all copies.

Permission to incorporate this software into commercial products may be obtained 
by contacting the University of California. 

Eveline Mumenthaler
Office of Intellectual Property Administration
10920 Wilshire Blvd., Suite 1200
Los Angeles, CA 90024-1406
310-794-0212

This software program and documentation are copyrighted by The Regents of the 
University of California. The software program and documentation are supplied 
"as is", without any accompanying services from The Regents. The Regents does 
not warrant that the operation of the program will be uninterrupted or 
error-free. The end-user understands that the program was developed for research 
purposes and is advised not to rely exclusively on the program for any reason.

IN NO EVENT SHALL THE UNIVERSITY OF CALIFORNIA BE LIABLE TO ANY PARTY FOR 
DIRECT, INDIRECT, SPECIAL, INCIDENTAL, OR CONSEQUENTIAL DAMAGES, INCLUDING LOST 
PROFITS, ARISING OUT OF THE USE OF THIS SOFTWARE AND ITS DOCUMENTATION, EVEN IF 
THE UNIVERSITY OF CALIFORNIA HAS BEEN ADVISED OF THE POSSIBILITY OF SUCH DAMAGE. 
THE UNIVERSITY OF CALIFORNIA SPECIFICALLY DISCLAIMS ANY WARRANTIES, INCLUDING, 
BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A 
PARTICULAR PURPOSE. THE SOFTWARE PROVIDED HEREUNDER IS ON AN "AS IS" BASIS, AND 
THE UNIVERSITY OF CALIFORNIA HAS NO OBLIGATIONS TO PROVIDE MAINTENANCE, SUPPORT, 
UPDATES, ENHANCEMENTS, OR MODIFICATIONS. 
*/

package edu.ucla.library.libservices;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import javax.mail.internet.MimeMessage;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;

import org.subethamail.wiser.Wiser;
import org.subethamail.wiser.WiserMessage;


public class SmtpLogger
{
  Wiser wiser;
  Timer timer;
  FileWriter fw;
  String logFile;

  public SmtpLogger(String port, String logFile)
    throws IOException
  {
    this.logFile = logFile;
    wiser = new Wiser();
    wiser.setPort( Integer.parseInt(port) );
    wiser.start();
    timer = new Timer();
    // write out to the log every ten seconds
    timer.schedule( new LogMessagesTask(), 10 * 1000, 10 * 1000 ); 
  }
  
  public void logMessage(BufferedWriter log, MimeMessage msg)
  throws Exception
  {
    String to = msg.getHeader("To", null);
    String subject = msg.getSubject();
    String body = (String)msg.getContent();
    log.write("--------------------");
    log.newLine();
    log.write("To: " + to);
    log.newLine();
    log.write("Subject: " + subject);
    log.newLine();
    log.newLine();
    log.write(body);
    log.newLine();
  }
  
  public void logMessages()
  throws Exception
  {
    List<WiserMessage> messages = wiser.getMessages();
    if (messages.size() > 0)
    {
      FileWriter fw = new FileWriter (logFile, true) ; 
      BufferedWriter log = new BufferedWriter(fw);
      for ( WiserMessage message: wiser.getMessages() )
      {
        String envelopeSender = message.getEnvelopeSender();
        String envelopeReceiver = message.getEnvelopeReceiver();

        MimeMessage msg;
        msg = message.getMimeMessage();
        logMessage(log, msg);
      }
      log.close();
      messages.clear();
    }
  }

  class LogMessagesTask
    extends TimerTask
  {
    public void run()
    {
      try
      {
        logMessages();
      }
      catch ( Exception e )
      {
        System.out.println("Caught exception: " + e.toString());
      }
    }
  }

  public static void main( String[] args )
    throws IOException, ParseException
  {
    String port = "25";
    String logFile = "smtp.log";
    Options options = new Options();
    options.addOption("h", false, "show help");
    options.addOption("p", true, "port");
    options.addOption("l", true, "log file");
    CommandLineParser parser = new PosixParser();
    CommandLine cmd = parser.parse( options, args);
    if (cmd.hasOption("h"))
    {
      HelpFormatter formatter = new HelpFormatter();
      formatter.printHelp( "SmtpLogger", options);
    }
    else
    {
      if (cmd.hasOption("p"))
      {
        port = cmd.getOptionValue("p");
      }
      if (cmd.hasOption("l"))
      {
        logFile = cmd.getOptionValue("l");
      }
      new SmtpLogger(port, logFile);
    }
  }
}
