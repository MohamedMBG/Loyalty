import express from 'express';
import dotenv from 'dotenv';
import nodemailer from 'nodemailer';
import { v4 as uuidv4 } from 'uuid';
import crypto from 'node:crypto';
import path from 'node:path';
import fs from 'node:fs/promises';

dotenv.config();

const requiredEnvVars = [
  'PORT',
  'BASE_URL',
  'APP_SUCCESS_URL',
  'SMTP_HOST',
  'SMTP_PORT',
  'SMTP_USER',
  'SMTP_PASS',
  'SMTP_FROM'
];

const missingEnv = requiredEnvVars.filter((key) => !process.env[key] || process.env[key].trim() === '');
if (missingEnv.length > 0) {
  console.error(`Missing required environment variables: ${missingEnv.join(', ')}`);
  process.exit(1);
}

const DATA_DIR = process.env.DATA_DIR || path.join(process.cwd(), 'data');
const TOKEN_FILE_PATH = path.join(DATA_DIR, 'tokens.json');
const USERS_FILE_PATH = path.join(DATA_DIR, 'users.json');

const readJsonFile = async (filePath, defaultValue) => {
  try {
    const data = await fs.readFile(filePath, 'utf8');
    return JSON.parse(data);
  } catch (error) {
    if (error.code === 'ENOENT') {
      return defaultValue;
    }
    throw error;
  }
};

const writeJsonFile = async (filePath, value) => {
  await fs.mkdir(path.dirname(filePath), { recursive: true });
  await fs.writeFile(filePath, JSON.stringify(value, null, 2), 'utf8');
};

const loadTokens = async () => readJsonFile(TOKEN_FILE_PATH, {});
const saveTokens = async (tokens) => writeJsonFile(TOKEN_FILE_PATH, tokens);

const loadUsers = async () => readJsonFile(USERS_FILE_PATH, {});
const saveUsers = async (users) => writeJsonFile(USERS_FILE_PATH, users);

const transporter = nodemailer.createTransport({
  host: process.env.SMTP_HOST,
  port: Number(process.env.SMTP_PORT),
  secure: process.env.SMTP_SECURE === 'true' || process.env.SMTP_SECURE === '1' || Number(process.env.SMTP_PORT) === 465,
  auth: {
    user: process.env.SMTP_USER,
    pass: process.env.SMTP_PASS
  }
});

transporter.verify()
  .then(() => console.log('SMTP connection established successfully.'))
  .catch((err) => {
    console.error('Failed to verify SMTP transporter. Double-check credentials and network.', err);
  });

const app = express();
app.use(express.json());

const DEFAULT_TOKEN_TTL_MINUTES = Number(process.env.VERIFICATION_TOKEN_TTL_MINUTES || 60 * 24);

const buildEmailHtml = (verificationLink, recipientEmail) => `
<!DOCTYPE html>
<html lang="en">
  <head>
    <meta charset="UTF-8" />
    <meta name="viewport" content="width=device-width, initial-scale=1.0" />
    <title>Verify your email</title>
    <style>
      :root {
        color-scheme: light;
      }
      body {
        font-family: 'Segoe UI', Roboto, Helvetica, Arial, sans-serif;
        background: #f5f7fb;
        margin: 0;
        padding: 0;
        color: #1f2937;
      }
      .wrapper {
        width: 100%;
        padding: 32px 0;
      }
      .container {
        max-width: 480px;
        margin: 0 auto;
        background: #ffffff;
        border-radius: 16px;
        box-shadow: 0 30px 60px rgba(15, 23, 42, 0.08);
        overflow: hidden;
      }
      .header {
        background: linear-gradient(135deg, #2563eb, #4f46e5);
        padding: 32px;
        text-align: center;
        color: #ffffff;
      }
      .header h1 {
        margin: 0;
        font-size: 24px;
        font-weight: 700;
      }
      .content {
        padding: 32px;
      }
      .cta-button {
        display: inline-block;
        background: #2563eb;
        color: #ffffff;
        text-decoration: none;
        padding: 14px 28px;
        border-radius: 999px;
        font-weight: 600;
        font-size: 16px;
        letter-spacing: 0.3px;
        box-shadow: 0 20px 30px rgba(37, 99, 235, 0.25);
      }
      .cta-button:hover {
        background: #1d4ed8;
      }
      .footer {
        padding: 24px 32px 32px;
        font-size: 12px;
        color: #6b7280;
        text-align: center;
      }
      .divider {
        height: 1px;
        background: #e5e7eb;
        margin: 24px 0;
      }
      @media (max-width: 600px) {
        .container {
          margin: 0 16px;
        }
        .content {
          padding: 24px;
        }
      }
    </style>
  </head>
  <body>
    <div class="wrapper">
      <div class="container">
        <div class="header">
          <h1>Confirm your email address</h1>
        </div>
        <div class="content">
          <p>Hi there,</p>
          <p>
            Thanks for joining our loyalty program! Please confirm that
            <strong>${recipientEmail}</strong> is your email address by clicking the button below.
          </p>
          <p style="text-align: center; margin: 32px 0;">
            <a class="cta-button" href="${verificationLink}" target="_blank" rel="noopener">
              Verify email
            </a>
          </p>
          <p>
            This verification link will expire in ${DEFAULT_TOKEN_TTL_MINUTES / 60 >= 1 ? `${DEFAULT_TOKEN_TTL_MINUTES / 60} hours` : `${DEFAULT_TOKEN_TTL_MINUTES} minutes`}.
            If you did not create an account, you can safely ignore this message.
          </p>
          <div class="divider"></div>
          <p style="font-size: 13px; color: #4b5563;">
            Or copy and paste this link into your browser:
            <br />
            <a href="${verificationLink}" style="color: #2563eb; word-break: break-all;">${verificationLink}</a>
          </p>
        </div>
        <div class="footer">
          <p>Sent securely from your Loyalty App team.</p>
        </div>
      </div>
    </div>
  </body>
</html>
`;

app.get('/healthz', (_, res) => {
  res.status(200).json({ status: 'ok' });
});

app.post('/auth/send-verification', async (req, res) => {
  try {
    const { uid, email } = req.body || {};

    if (!uid || typeof uid !== 'string' || !email || typeof email !== 'string') {
      return res.status(400).json({ message: 'uid and email are required.' });
    }

    const normalizedEmail = email.trim().toLowerCase();
    const [tokens, users] = await Promise.all([loadTokens(), loadUsers()]);

    const existingUser = users[uid];

    if (existingUser && existingUser.email !== normalizedEmail) {
      return res.status(400).json({ message: 'Email does not match the existing user record.' });
    }

    if (existingUser?.verified) {
      return res.status(200).json({ message: 'Email is already verified.' });
    }

    const token = `${uuidv4()}${uuidv4()}`.replace(/-/g, '');
    const hashedToken = crypto.createHash('sha256').update(token).digest('hex');

    const expiresAt = new Date(Date.now() + DEFAULT_TOKEN_TTL_MINUTES * 60 * 1000);
    const nowIso = new Date().toISOString();

    tokens[hashedToken] = {
      uid,
      email: normalizedEmail,
      hash: hashedToken,
      expiresAt: expiresAt.toISOString(),
      used: false,
      createdAt: nowIso
    };

    users[uid] = {
      email: normalizedEmail,
      verified: false,
      createdAt: existingUser?.createdAt || nowIso,
      updatedAt: nowIso,
      verifiedAt: existingUser?.verifiedAt || null
    };

    await Promise.all([saveTokens(tokens), saveUsers(users)]);

    const verificationUrl = new URL('/auth/verify', process.env.BASE_URL);
    verificationUrl.searchParams.set('tid', token);
    verificationUrl.searchParams.set('ts', Date.now().toString());

    const mailOptions = {
      from: process.env.SMTP_FROM,
      to: normalizedEmail,
      subject: 'Verify your email address',
      html: buildEmailHtml(verificationUrl.toString(), normalizedEmail),
      text: `Hi!\n\nPlease verify your email address by visiting the following link: ${verificationUrl.toString()}\n\nIf you did not request this, you can ignore this message.`
    };

    await transporter.sendMail(mailOptions);

    return res.status(200).json({ message: 'Verification email sent.' });
  } catch (error) {
    console.error('Error sending verification email:', error);
    return res.status(500).json({ message: 'Failed to send verification email.' });
  }
});

app.get('/auth/status/:uid', async (req, res) => {
  const { uid } = req.params;

  if (!uid) {
    return res.status(400).json({ message: 'uid is required.' });
  }

  try {
    const users = await loadUsers();
    const user = users[uid];

    if (!user) {
      return res.status(404).json({ message: 'User not found.' });
    }

    return res.status(200).json({
      uid,
      email: user.email,
      verified: Boolean(user.verified),
      verifiedAt: user.verifiedAt,
      updatedAt: user.updatedAt
    });
  } catch (error) {
    console.error('Error retrieving user status:', error);
    return res.status(500).json({ message: 'Failed to retrieve verification status.' });
  }
});

app.get('/auth/verify', async (req, res) => {
  const { tid } = req.query;

  if (!tid || typeof tid !== 'string') {
    return res.status(400).send('Invalid verification link.');
  }

  const hashedToken = crypto.createHash('sha256').update(tid).digest('hex');

  try {
    const [tokens, users] = await Promise.all([loadTokens(), loadUsers()]);
    const tokenData = tokens[hashedToken];

    if (!tokenData) {
      return res.status(400).send('Verification link is invalid or has already been used.');
    }

    if (tokenData.used) {
      return res.redirect(process.env.APP_SUCCESS_URL);
    }

    const now = new Date();
    const nowIso = now.toISOString();

    if (new Date(tokenData.expiresAt) < now) {
      tokens[hashedToken] = {
        ...tokenData,
        used: true,
        usedAt: nowIso,
        invalidatedReason: 'expired'
      };
      await saveTokens(tokens);
      return res.status(410).send('Verification link has expired.');
    }

    tokens[hashedToken] = {
      ...tokenData,
      used: true,
      usedAt: nowIso,
      usedFromIp: req.ip,
      usedUserAgent: req.get('user-agent') || null
    };

    const existingUser = users[tokenData.uid] || { email: tokenData.email };
    users[tokenData.uid] = {
      ...existingUser,
      email: tokenData.email,
      verified: true,
      verifiedAt: nowIso,
      updatedAt: nowIso,
      createdAt: existingUser.createdAt || nowIso
    };

    await Promise.all([saveTokens(tokens), saveUsers(users)]);

    return res.redirect(process.env.APP_SUCCESS_URL);
  } catch (error) {
    console.error('Error verifying token:', error);
    return res.status(500).send('Something went wrong verifying your email.');
  }
});

const port = Number(process.env.PORT || 8080);
app.listen(port, () => {
  console.log(`Email verification service running on port ${port}`);
});

process.on('unhandledRejection', (reason) => {
  console.error('Unhandled promise rejection:', reason);
});

process.on('uncaughtException', (err) => {
  console.error('Uncaught exception:', err);
});
