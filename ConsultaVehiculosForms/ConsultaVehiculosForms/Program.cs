// =================================================================================================
// Archivo: Program.cs
// Proyecto: ConsultaVehiculosForms (Cliente Administrativo de Escritorio - Windows Forms)
// Propósito: Punto de entrada principal de la aplicación de escritorio Windows Forms encargada de
//            inicializar la configuración visual del entorno gráfico y lanzar el formulario
//            principal (Form1) de administración multicanal conectado al gateway NGINX (:8090).
// Actividades APE 04 que implementa:
//   - [Actividad 5]: Arranque del cliente administrativo Windows Forms (.NET 8 C#) que expone
//                    las pestañas Vehículos, Compradores y Ventas (solo consulta) vía NGINX (:8090).
// =================================================================================================

// [Actividad 5] Espacio de nombres raíz del proyecto cliente administrativo Windows Forms.
namespace ConsultaVehiculosForms
{
    // [Actividad 5] Clase estática interna que aloja el punto de entrada de la aplicación de escritorio.
    internal static class Program
    {
        /// <summary>
        /// [Actividad 5] Punto de entrada principal de la aplicación Windows Forms.
        /// Inicializa el entorno gráfico de escritorio y ejecuta el formulario administrativo Form1.
        /// </summary>
        // [Actividad 5] Atributo requerido por Windows Forms para configurar el hilo principal en modelo STA (Single-Threaded Apartment) compatible con controles COM/UI.
        [STAThread]
        // [Actividad 5] Método principal invocado por el runtime de .NET al iniciar el ejecutable.
        static void Main()
        {
            // [Actividad 5] Inicializa la configuración estándar de Windows Forms (soporte High DPI, estilos visuales modernos y fuente predeterminada).
            ApplicationConfiguration.Initialize();
            // [Actividad 5] Inicia el bucle de mensajes de la interfaz gráfica y muestra la ventana principal Form1 para operar sobre Vehículos, Compradores y Ventas.
            Application.Run(new Form1());
        }
    }
}
