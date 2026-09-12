// ================================
// Book Appointment
// ================================

function bookAppointment() {

    let name =
        document.getElementById("name").value;

    let age =
        document.getElementById("age").value;

    let gender =
        document.getElementById("gender").value;

    let phone =
        document.getElementById("phone").value;

    let email =
        document.getElementById("email").value;

    let problem =
        document.getElementById("problem").value;

    let doctor =
        document.getElementById("doctorName").value;

    let specialization =
        document.getElementById("specialization").value;

    let hospital =
        document.getElementById("hospital").value;

    let consultation =
        document.getElementById("consultation").value;

    let date =
        document.getElementById("date").value;

    let time =
        document.getElementById("time").value;

    let notes =
        document.getElementById("notes").value;

    let payment =
        document.querySelector(
            'input[name="payment"]:checked'
        ).value;
            if(name==""){

        alert("Enter Patient Name");

        return;

    }

    if(age==""){

        alert("Enter Age");

        return;

    }

    if(phone==""){

        alert("Enter Phone Number");

        return;

    }

    if(doctor==""){

        alert("Select Doctor");

        return;

    }

    if(date==""){

        alert("Select Date");

        return;

    }

    if(time==""){

        alert("Select Time");

        return;

    }
        let appointmentId =

        "MED"

        +

        Math.floor(

            100000

            +

            Math.random()*900000

        );
            document.getElementById("successSection").style.display="flex";

    document.getElementById("successName").innerHTML=name;

    document.getElementById("successDoctor").innerHTML=doctor;

    document.getElementById("successHospital").innerHTML=hospital;

    document.getElementById("successDate").innerHTML=date;

    document.getElementById("successTime").innerHTML=time;

    document.getElementById("successType").innerHTML=consultation;

    document.getElementById("appointmentId").innerHTML=appointmentId;
        window.scrollTo({

        top:

        document.getElementById("successSection").offsetTop,

        behavior:"smooth"

    });

}
// ================================
// Download Appointment Slip
// ================================

function downloadSlip(){

    window.print();

}

// ================================
// Auto-fill from URL or LocalStorage
// ================================
document.addEventListener("DOMContentLoaded", function () {
    let params = new URLSearchParams(window.location.search);
    let doctor = params.get("doctor") || localStorage.getItem("doctorName");
    let hospital = params.get("hospital") || localStorage.getItem("hospital");
    let specialization = params.get("specialization") || localStorage.getItem("specialization");

    if (doctor) {
        let docField = document.getElementById("doctorName");
        if (docField) docField.value = doctor;
    }
    if (hospital) {
        let hospField = document.getElementById("hospital");
        if (hospField) hospField.value = hospital;
    }
    if (specialization) {
        let specField = document.getElementById("specialization");
        if (specField) {
            for (let i = 0; i < specField.options.length; i++) {
                if (specField.options[i].text.toLowerCase() === specialization.toLowerCase() ||
                    specialization.toLowerCase().includes(specField.options[i].text.toLowerCase())) {
                    specField.selectedIndex = i;
                    break;
                }
            }
        }
    }
});

