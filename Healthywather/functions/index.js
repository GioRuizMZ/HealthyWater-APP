const functions = require("firebase-functions");
// const nodemailer = require("nodemailer");
const admin = require("firebase-admin");

admin.initializeApp();

exports.AvisoCitas = functions.database.ref("Citas/{uid}/{numeroCitas}/Estado")
    .onUpdate(async (change, context) => {
      const beforeData = change.before.val();
      const afterData = change.after.val();

      if (beforeData !== afterData) {
        const uid = context.params.uid;

        try {
          const userSnapshot = await admin.database()
              .ref(`usuarios/${uid}/fcmtoken`).once("value");

          const fcmToken = userSnapshot.val();

          if (fcmToken) {
            let payload;

            if (afterData === "Aceptada") {
              payload = {
                notification: {
                  title: "Cita aceptada",
                  body: "Tu cita ha sido aceptada,"+
                  "Espera a que se realize el servicio",
                },
              };
            } else if (afterData === "En camino") {
              payload = {
                notification: {
                  title: "Personal en camino",
                  body: "El personal para tu cita ya está en"+
                                "camino hacia su domicilio, ¡prepárese!",
                },
              };
            }

            if (payload) {
              await admin.messaging().sendToDevice(fcmToken, payload);
              console.log("Notificación enviada exitosamente:"
                  , payload.notification.title);
            } else {
              console.log("No se envió ninguna notificación"+
               "ya que el estado no es relevante:", afterData);
            }
          } else {
            console.log("No se encontró el token FCM para el usuario:"
                , uid);
          }
        } catch (error) {
          console.error("Error al enviar la notificación:", error);
        }
      }
    });

exports.AvisoVariables = functions.database
    .ref("Dispositivos/{dispositivoId}/{variable}")
    .onWrite(async (change, context) => {
      const dispositivoId = context.params.dispositivoId;
      const variable = context.params.variable;
      const newValue = change.after.val();
      const currentTime = Date.now();

      try {
        const uidSnapshot = await admin.database()
            .ref("usuarios")
            .orderByChild("Dispositivos")
            .equalTo(dispositivoId)
            .once("value");
        const users = uidSnapshot.val();

        if (users) {
          for (const uid of Object.keys(users)) {
            const userNotificationRef = admin.database()
                .ref(`/usuarios/${uid}/notificaciones/alertas`);
            const notificationPreferenceSnapshot = await
            userNotificationRef.once("value");
            const notificationPreference = notificationPreferenceSnapshot.val();

            if (notificationPreference) {
              const userModeRef = admin.database()
                  .ref(`/usuarios/${uid}/notificaciones/Modo`);
              const userModeSnapshot = await userModeRef.once("value");
              const userMode = userModeSnapshot.val();

              const lastNotificationRef = admin.database()
                  .ref(`/usuarios/${uid}/notificaciones/lastNotification`);
              const lastNotificationSnapshot = await
              lastNotificationRef.once("value");
              const lastNotificationTime = lastNotificationSnapshot.val() || 0;

              if (currentTime - lastNotificationTime < 3600000) {
                console.log(`No se enviará notificación al usuario ${uid}
                porque no ha pasado una hora desde la última notificación.`);
                return null;
              }

              let payload = null;
              if (userMode === "P") {
                // Modo P - Agua potable
                if ((variable === "ppm_e" || variable === "ppm_s") &&
                  newValue > 500) {
                  payload = {
                    notification: {
                      title: "Alerta de Contaminación",
                      body: "El ppm está muy alto para agua potable,"+
                      "su agua está contaminada.",
                    },
                    data: {
                      type: "alerta",
                    },
                  };
                } else if ((variable === "turbidez_e" ||
                variable === "turbidez_s") && newValue > 100) {
                  payload = {
                    notification: {
                      title: "Alerta de Contaminación",
                      body: "La turbidez está muy alta para agua potable,"+
                      "su agua está contaminada.",
                    },
                    data: {
                      type: "alerta",
                    },
                  };
                }
              } else if (userMode === "D") {
                if ((variable === "ppm_e" || variable === "ppm_s") &&
                  newValue > 750) {
                  payload = {
                    notification: {
                      title: "Alerta de Contaminación",
                      body: "El ppm está muy alto para uso doméstico,"+
                      "su agua está contaminada.",
                    },
                    data: {
                      type: "alerta",
                    },
                  };
                } else if ((variable === "turbidez_e" ||
                variable === "turbidez_s") && newValue > 450) {
                  payload = {
                    notification: {
                      title: "Alerta de Contaminación",
                      body: "La turbidez está muy alta para uso doméstico,"+
                      "su agua está contaminada.",
                    },
                    data: {
                      type: "alerta",
                    },
                  };
                }
              } else if (userMode === "A") {
                if ((variable === "ppm_e" || variable === "ppm_s") &&
                  newValue > 5) {
                  payload = {
                    notification: {
                      title: "Alerta de Contaminación",
                      body: "El ppm está muy alto para una alberca,"+
                      "su agua está contaminada.",
                    },
                    data: {
                      type: "alerta",
                    },
                  };
                } else if ((variable === "turbidez_e" ||
                variable === "turbidez_s") && newValue > 0.5) {
                  payload = {
                    notification: {
                      title: "Alerta de Contaminación",
                      body: "La turbidez está muy alta para una alberca,"+
                      "su agua está contaminada.",
                    },
                    data: {
                      type: "alerta",
                    },
                  };
                } else if ((variable === "ph_e" || variable === "ph_s") &&
                  (newValue < 7.0 || newValue > 7.5)) {
                  payload = {
                    notification: {
                      title: "Alerta de pH",
                      body: "El pH está fuera del rango ideal para tu alberca,"+
                      "su agua está contaminada.",
                    },
                    data: {
                      type: "alerta",
                    },
                  };
                }
              }

              if (payload) {
                const tokenSnapshot = await admin.database()
                    .ref(`/usuarios/${uid}/fcmtoken`)
                    .once("value");
                const token = tokenSnapshot.val();
                if (token) {
                  await admin.messaging().sendToDevice(token, payload);
                  console.log(`Se envió la notificación al usuario 
                  ${uid} correctamente.`);
                  // Actualizar la última marca de tiempo de notificación
                  await lastNotificationRef.set(currentTime);
                } else {
                  console.log(`No se encontró el token FCM para el usuario 
                  ${uid}.`);
                }
              } else {
                console.log(`No se cumple ninguna condición de 
                alerta para el usuario ${uid}.`);
              }
            } else {
              console.log(`El usuario ${uid} 
              ha deshabilitado las notificaciones de alerta.`);
            }
          }
        } else {
          console.log(`No se encontró el usuario correspondiente para el 
          dispositivo ${dispositivoId}`);
        }
      } catch (error) {
        console.error("Error al enviar la notificación:", error);
      }

      return null;
    });


/* const transporter = nodemailer.createTransport({
  service: "gmail",
  auth: {
    user: functions.config().email.email,
    pass: functions.config().email.password,
  },
});

 exports.sendCartEmail = functions.https.onRequest((req, res) => {
  const {email, cartItems} = req.body;

  const mailOptions = {
    from: functions.config().email.email,
    to: email,
    subject: "Detalles de tu compra",
    html: `
      <div style="font-family: Arial,
      sans-serif; line-height: 1.6; color: #333;">
        <h2>Gracias por tu compra</h2>
        <p>A continuación se muestran los detalles
        de los productos que compraste:</p>
        <ul>
          ${cartItems.split("\n").map((item) => `<li>${item}</li>`).join("")}
        </ul>
        <a href="#" style="display: inline-block; margin-top: 20px;
        padding: 10px 20px; background-color: #007BFF; color: #fff;
        text-decoration: none; border-radius: 5px;">Ver Pedido</a>
      </div>
    `,
  };

  transporter.sendMail(mailOptions, (error, info) => {
    if (error) {
      return res.status(500).send(error.toString());
    }
    return res.status(200).send("Correo enviado: " + info.response);
  });
});*/
