package app.myliberty.easy_records.organizations.rest.endpoints

import app.myliberty.easy_records.organizations.business.retail.stock_movement_reason.api.StockMovementReasonInsertDto
import app.myliberty.easy_records.organizations.business.retail.stock_movement_reason.api.StockMovementReasonResponseDto
import app.myliberty.easy_records.organizations.business.retail.stock_movement_reason.api.StockMovementReasonService
import app.myliberty.easy_records.organizations.business.retail.stock_movement_reason.api.StockMovementReasonUpdateDto
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatusCode
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@CrossOrigin
@RestController
@RequestMapping("secured/stock-movement-reasons")
class StockMovementReasonEndpoint(
    private val stockMovementReasonService: StockMovementReasonService
) {

    @GetMapping
    fun getAll(): Collection<StockMovementReasonResponseDto> =
        stockMovementReasonService.getAll()

    @PostMapping
    fun create(@RequestBody stockMovementReasonInsertDto: StockMovementReasonInsertDto): StockMovementReasonResponseDto =
        stockMovementReasonService.create(stockMovementReasonInsertDto)

    @PutMapping
    fun update(@RequestBody stockMovementReasonUpdateDto: StockMovementReasonUpdateDto): StockMovementReasonResponseDto =
        stockMovementReasonService.update(stockMovementReasonUpdateDto)

    @DeleteMapping("{id}")
    fun delete(@PathVariable id: UUID): ResponseEntity<HttpStatusCode> {
        stockMovementReasonService.delete(id)
        return ResponseEntity(HttpStatus.NO_CONTENT)
    }
}
