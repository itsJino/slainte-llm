import { Message } from '@/models/message';

// Format the conversation for email
export const formatConversationForEmail = (messages: Message[]): string => {
  const timestamp = new Date().toLocaleString();
  let emailContent = `
<!DOCTYPE html>
<html>
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>Slainte Health Chat Conversation</title>
  <style>
    body {
      font-family: Arial, sans-serif;
      line-height: 1.6;
      color: #333;
      max-width: 600px;
      margin: 0 auto;
      padding: 20px;
    }
    .header {
      border-bottom: 2px solid #006354;
      padding-bottom: 10px;
      margin-bottom: 20px;
      display: flex;
      justify-content: space-between;
      align-items: center;
    }
    .header h1 {
      color: #006354;
      margin: 0;
    }
    .timestamp {
      color: #666;
      font-size: 14px;
    }
    .message {
      margin-bottom: 15px;
      max-width: 80%;
    }
    .user-message {
      margin-left: auto;
      background-color: #f3f3f3;
      padding: 10px;
      border-radius: 8px;
    }
    .ai-message {
      margin-right: auto;
      background-color: #006354;
      color: white;
      padding: 10px;
      border-radius: 8px;
    }
    .message-time {
      font-size: 10px;
      text-align: right;
      margin-top: 5px;
    }
    .user-time {
      color: #666;
    }
    .ai-time {
      color: #eee;
    }
    .footer {
      margin-top: 30px;
      padding-top: 10px;
      border-top: 1px solid #eee;
      font-size: 12px;
      color: #666;
      text-align: center;
    }
  </style>
</head>
<body>
  <div class="header">
    <h1>Slainte Chat Conversation</h1>
    <div class="timestamp">${timestamp}</div>
  </div>
  <div class="conversation">
`;

  // Add each message to the email content
  messages.forEach(message => {
    if (message.role === 'user') {
      emailContent += `
    <div class="message user-message">
      <div>${message.content}</div>
      <div class="message-time user-time">${message.timestamp}</div>
    </div>
`;
    } else if (message.role === 'ai') {
      emailContent += `
    <div class="message ai-message">
      <div>${message.content}</div>
      <div class="message-time ai-time">${message.timestamp}</div>
    </div>
`;
    }
  });

  // Add footer
  emailContent += `
  </div>
  <div class="footer">
    <p>This is an automated email from Slainte Health Chat. Please do not reply to this email.</p>
    <p>For medical emergencies, please call emergency services immediately.</p>
  </div>
</body>
</html>
`;

  return emailContent;
};

// Function to send email (to be implemented on the backend)
export const sendConversationByEmail = async (
  email: string, 
  messages: Message[]
): Promise<boolean> => {
  try {
    // This would be implemented on your backend
    // For now, we'll simulate a successful API call
    const emailContent = formatConversationForEmail(messages);
    
    // Here you would make an API call to your email sending endpoint
    // const response = await fetch('/api/send-email', {
    //   method: 'POST',
    //   headers: { 'Content-Type': 'application/json' },
    //   body: JSON.stringify({ email, content: emailContent }),
    // });
    
    // if (!response.ok) throw new Error('Failed to send email');
    // return true;
    
    // For now, simulate a successful response
    await new Promise(resolve => setTimeout(resolve, 1000));
    return true;
  } catch (error) {
    console.error('Error sending email:', error);
    return false;
  }
};