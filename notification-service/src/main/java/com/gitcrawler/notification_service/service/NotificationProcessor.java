
package com.gitcrawler.notification_service.service;

import com.gitcrawler.notification_service.dto.NotificationEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class NotificationProcessor {

    private final EmailService emailService;

    public void process(NotificationEvent event) {

        boolean success = "SUCCESS".equalsIgnoreCase(event.status());

        String subject = success
                ? "GitCrawler | Repository Indexing Completed"
                : "GitCrawler | Repository Indexing Failed";

        String body = buildEmailTemplate(event, success);

        emailService.sendEmail(
                event.email(),
                subject,
                body
        );
    }

    private String buildEmailTemplate(
            NotificationEvent event,
            boolean success
    ) {
        String repository = escapeHtml(event.repositoryName());

        String status = success ? "COMPLETED" : "FAILED";
        String statusColor = success ? "#15803d" : "#dc2626";
        String statusBackground = success ? "#dcfce7" : "#fee2e2";

        String heading = success
                ? "Repository indexing completed!"
                : "Repository indexing failed";

        String description = success
                ? "Great news! Your repository has been successfully indexed and is ready to use."
                : "We couldn't complete indexing your repository. Please review the details below.";

        String failureSection = "";

        if (!success) {
            String reason = escapeHtml(
                    event.failureReason() == null
                            || event.failureReason().isBlank()
                            ? "Reason not provided"
                            : event.failureReason()
            );

            failureSection = """
                    <tr>
                        <td style="padding: 20px 0 0;">
                            <div style="
                                background: #fff7ed;
                                border: 1px solid #fed7aa;
                                border-radius: 8px;
                                padding: 16px;
                            ">
                                <p style="
                                    margin: 0 0 8px;
                                    color: #9a3412;
                                    font-size: 13px;
                                    font-weight: 600;
                                ">
                                    FAILURE REASON
                                </p>
                                <p style="
                                    margin: 0;
                                    color: #7c2d12;
                                    font-size: 14px;
                                    line-height: 1.6;
                                    overflow-wrap: anywhere;
                                ">%s</p>
                            </div>
                        </td>
                    </tr>
                    """.formatted(reason);
        }

        return """
                <!DOCTYPE html>
                <html>
                <head>
                    <meta charset="UTF-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                </head>

                <body style="
                    margin: 0;
                    padding: 30px 12px;
                    background-color: #f4f4f5;
                    font-family: Arial, Helvetica, sans-serif;
                ">

                    <table role="presentation" width="100%%"
                           cellspacing="0" cellpadding="0" border="0">
                        <tr>
                            <td align="center">

                                <table role="presentation"
                                       width="100%%"
                                       cellspacing="0"
                                       cellpadding="0"
                                       border="0"
                                       style="
                                           max-width: 560px;
                                           background: #ffffff;
                                           border-radius: 12px;
                                           overflow: hidden;
                                           border: 1px solid #e4e4e7;
                                       ">

                                    <!-- Header -->
                                    <tr>
                                        <td style="
                                            padding: 24px 32px;
                                            background: #18181b;
                                        ">
                                            <span style="
                                                color: #ffffff;
                                                font-size: 22px;
                                                font-weight: 700;
                                                letter-spacing: -0.5px;
                                            ">GitCrawler</span>

                                            <span style="
                                                display: block;
                                                margin-top: 5px;
                                                color: #a1a1aa;
                                                font-size: 12px;
                                            ">Repository indexing notification</span>
                                        </td>
                                    </tr>

                                    <!-- Main Content -->
                                    <tr>
                                        <td style="padding: 32px;">

                                            <div style="
                                                display: inline-block;
                                                padding: 7px 12px;
                                                border-radius: 20px;
                                                background: %s;
                                                color: %s;
                                                font-size: 11px;
                                                font-weight: 700;
                                                letter-spacing: 0.5px;
                                            ">%s</div>

                                            <h2 style="
                                                margin: 20px 0 10px;
                                                color: #18181b;
                                                font-size: 22px;
                                                line-height: 1.4;
                                            ">%s</h2>

                                            <p style="
                                                margin: 0;
                                                color: #52525b;
                                                font-size: 14px;
                                                line-height: 1.7;
                                            ">%s</p>

                                            <!-- Repository Details -->
                                            <table role="presentation"
                                                   width="100%%"
                                                   cellspacing="0"
                                                   cellpadding="0"
                                                   style="
                                                       margin-top: 26px;
                                                       background: #fafafa;
                                                       border: 1px solid #e4e4e7;
                                                       border-radius: 8px;
                                                   ">
                                                <tr>
                                                    <td style="padding: 18px;">
                                                        <p style="
                                                            margin: 0 0 8px;
                                                            color: #71717a;
                                                            font-size: 12px;
                                                            font-weight: 600;
                                                        ">REPOSITORY</p>

                                                        <p style="
                                                            margin: 0;
                                                            color: #18181b;
                                                            font-size: 15px;
                                                            font-weight: 600;
                                                            overflow-wrap: anywhere;
                                                        ">%s</p>

                                                        <div style="
                                                            margin-top: 16px;
                                                            border-top: 1px solid #e4e4e7;
                                                            padding-top: 14px;
                                                        ">
                                                            <span style="
                                                                color: #71717a;
                                                                font-size: 12px;
                                                            ">Status: </span>

                                                            <span style="
                                                                color: %s;
                                                                font-size: 13px;
                                                                font-weight: 700;
                                                            ">%s</span>
                                                        </div>
                                                    </td>
                                                </tr>
                                            </table>

                                            %s

                                        </td>
                                    </tr>

                                    <!-- Footer -->
                                    <tr>
                                        <td style="
                                            padding: 20px 32px;
                                            background: #fafafa;
                                            border-top: 1px solid #e4e4e7;
                                        ">
                                            <p style="
                                                margin: 0;
                                                color: #71717a;
                                                font-size: 12px;
                                                line-height: 1.6;
                                                text-align: center;
                                            ">
                                                This is an automated notification from GitCrawler.
                                                <br>
                                                Happy coding!
                                            </p>
                                        </td>
                                    </tr>

                                </table>

                            </td>
                        </tr>
                    </table>

                </body>
                </html>
                """.formatted(
                statusBackground,
                statusColor,
                status,
                heading,
                description,
                repository,
                statusColor,
                status,
                failureSection
        );
    }

    private String escapeHtml(String value) {
        if (value == null) {
            return "";
        }

        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}
